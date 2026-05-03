package com.example.attendancetrackerapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DataBase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "attendance.db";
    private static final int DATABASE_VERSION = 6;

    public DataBase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "email TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL," +
                "profile_pic TEXT," +
                "expected_start TEXT DEFAULT '08:00'," +
                "expected_end TEXT DEFAULT '17:00'" +
                ")");

        db.execSQL("CREATE TABLE classes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "subject_name TEXT NOT NULL," +
                "section TEXT NOT NULL," +
                "teacher_id INTEGER NOT NULL," +
                "FOREIGN KEY (teacher_id) REFERENCES users(id)" +
                ")");

        db.execSQL("CREATE TABLE students (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "student_number TEXT UNIQUE NOT NULL," +
                "name TEXT NOT NULL," +
                "class_id INTEGER NOT NULL," +
                "date_added TEXT," +
                "FOREIGN KEY (class_id) REFERENCES classes(id)" +
                ")");

        db.execSQL("CREATE TABLE attendance (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "student_id INTEGER NOT NULL," +
                "student_name TEXT," +
                "class_name TEXT," +
                "teacher_id INTEGER," +
                "teacher_name TEXT," +
                "date TEXT NOT NULL," +
                "status TEXT NOT NULL," +
                "FOREIGN KEY (student_id) REFERENCES students(id)," +
                "UNIQUE(student_id, date)" +
                ")");

        db.execSQL("CREATE TABLE teacher_attendance (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "teacher_id INTEGER NOT NULL," +
                "date TEXT NOT NULL," +
                "clock_in TEXT," +
                "clock_out TEXT," +
                "status TEXT DEFAULT 'absent'," +
                "FOREIGN KEY (teacher_id) REFERENCES users(id)" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS teacher_attendance");
        db.execSQL("DROP TABLE IF EXISTS attendance");
        db.execSQL("DROP TABLE IF EXISTS students");
        db.execSQL("DROP TABLE IF EXISTS classes");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    // --- TEACHER ATTENDANCE METHODS ---

    public boolean clockIn(int teacherId, String date, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor check = db.rawQuery(
                "SELECT id FROM teacher_attendance WHERE teacher_id=? AND date=?",
                new String[]{String.valueOf(teacherId), date});
        if (check.getCount() > 0) {
            check.close();
            db.close();
            return false;
        }
        check.close();
        ContentValues values = new ContentValues();
        values.put("teacher_id", teacherId);
        values.put("date", date);
        values.put("clock_in", time);
        values.put("status", "present");
        long result = db.insert("teacher_attendance", null, values);
        db.close();
        return result != -1;
    }

    public boolean clockOut(int teacherId, String date, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // Check if already clocked out
        Cursor check = db.rawQuery(
                "SELECT clock_out FROM teacher_attendance WHERE teacher_id=? AND date=?",
                new String[]{String.valueOf(teacherId), date});
        if (check.moveToFirst()) {
            String existingClockOut = check.getString(0);
            if (existingClockOut != null && !existingClockOut.isEmpty()) {
                check.close();
                db.close();
                return false; // Already clocked out
            }
        }
        check.close();

        ContentValues values = new ContentValues();
        values.put("clock_out", time);
        int rows = db.update("teacher_attendance", values,
                "teacher_id=? AND date=?",
                new String[]{String.valueOf(teacherId), date});
        db.close();
        return rows > 0;
    }

    public boolean updateTeacherAttendanceStatus(int teacherId, String date, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status);
        int rows = db.update("teacher_attendance", values,
                "teacher_id=? AND date=?",
                new String[]{String.valueOf(teacherId), date});
        db.close();
        return rows > 0;
    }

    public Cursor getTeacherRecordForToday(int teacherId, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM teacher_attendance WHERE teacher_id=? AND date=?",
                new String[]{String.valueOf(teacherId), date});
    }

    // --- USER METHODS ---

    public boolean registerUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("password", password);
        long result = db.insert("users", null, values);
        db.close();
        return result != -1;
    }

    public boolean isEmailRegistered(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM users WHERE email=?", new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public Cursor loginUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM users WHERE email=? AND password=?",
                new String[]{email, password});
    }

    public boolean deleteUser(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete dependent data first
        db.delete("teacher_attendance", "teacher_id=?", new String[]{String.valueOf(userId)});
        
        // Find classes by this teacher to delete their students and attendance
        Cursor classes = db.rawQuery("SELECT id FROM classes WHERE teacher_id=?", new String[]{String.valueOf(userId)});
        if (classes.moveToFirst()) {
            do {
                int classId = classes.getInt(0);
                // Delete students' attendance for this class
                db.execSQL("DELETE FROM attendance WHERE student_id IN (SELECT id FROM students WHERE class_id=?)", new Object[]{classId});
                // Delete students in this class
                db.delete("students", "class_id=?", new String[]{String.valueOf(classId)});
            } while (classes.moveToNext());
        }
        classes.close();
        
        db.delete("classes", "teacher_id=?", new String[]{String.valueOf(userId)});
        int rows = db.delete("users", "id=?", new String[]{String.valueOf(userId)});
        db.close();
        return rows > 0;
    }

    // --- STUDENT METHODS ---

    // --- SUBJECT/CLASS METHODS ---

    public boolean addClass(String subjectName, String section, int teacherId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("subject_name", subjectName);
        values.put("section", section);
        values.put("teacher_id", teacherId);
        long result = db.insert("classes", null, values);
        db.close();
        return result != -1;
    }

    public Cursor getClassesByTeacher(int teacherId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM classes WHERE teacher_id=?",
                new String[]{String.valueOf(teacherId)});
    }

    public boolean deleteClass(int classId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("students", "class_id=?", new String[]{String.valueOf(classId)});
        int rows = db.delete("classes", "id=?", new String[]{String.valueOf(classId)});
        db.close();
        return rows > 0;
    }

// --- UPDATED STUDENT METHODS ---

    public boolean addStudentToClass(String studentNumber, String name, int classId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("student_number", studentNumber);
        values.put("name", name);
        values.put("class_id", classId);
        
        String timestamp = new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
        values.put("date_added", timestamp);

        long result = db.insert("students", null, values);
        db.close();
        return result != -1;
    }

    public Cursor getStudentsByClass(int classId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM students WHERE class_id=? ORDER BY student_number ASC",
                new String[]{String.valueOf(classId)});
    }

    public boolean deleteStudentFromClass(int studentId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("attendance", "student_id=?",
                new String[]{String.valueOf(studentId)});
        int rows = db.delete("students", "id=?",
                new String[]{String.valueOf(studentId)});
        db.close();
        return rows > 0;
    }

// --- UPDATE USER PROFILE ---

    public boolean updateUserProfile(int userId, String name,
                                     String password, String profilePic) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        if (!password.isEmpty()) values.put("password", password);
        if (profilePic != null) values.put("profile_pic", profilePic);
        int rows = db.update("users", values, "id=?",
                new String[]{String.valueOf(userId)});
        db.close();
        return rows > 0;
    }

    public Cursor getUserById(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM users WHERE id=?",
                new String[]{String.valueOf(userId)});
    }

    public Cursor findStudentByNumber(String studentNumber, int teacherId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT s.*, c.subject_name, c.section FROM students s " +
                "JOIN classes c ON s.class_id = c.id " +
                "WHERE s.student_number=? AND c.teacher_id=?",
                new String[]{studentNumber, String.valueOf(teacherId)});
    }

    // --- ATTENDANCE METHODS ---

    public boolean markAttendance(int studentId, String studentName, String className, int teacherId, String teacherName, String date, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("student_id", studentId);
        values.put("student_name", studentName);
        values.put("class_name", className);
        values.put("teacher_id", teacherId);
        values.put("teacher_name", teacherName);
        values.put("date", date);
        values.put("status", status);
        
        long result = db.insertWithOnConflict("attendance", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return result != -1;
    }

    public Cursor getAttendanceByDate(String date, int teacherId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT s.name, s.student_number, a.status " +
                        "FROM attendance a " +
                        "JOIN students s ON a.student_id = s.id " +
                        "JOIN classes c ON s.class_id = c.id " +
                        "WHERE a.date=? AND c.teacher_id=? " +
                        "ORDER BY s.student_number ASC",
                new String[]{date, String.valueOf(teacherId)});
    }

    public Cursor getAttendanceSummary(int teacherId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT s.name, s.student_number, " +
                        "COUNT(CASE WHEN a.status='present' THEN 1 END) as present_count, " +
                        "COUNT(a.id) as total_count " +
                        "FROM students s " +
                        "JOIN classes c ON s.class_id = c.id " +
                        "LEFT JOIN attendance a ON s.id = a.student_id " +
                        "WHERE c.teacher_id=? " +
                        "GROUP BY s.id ORDER BY s.student_number ASC",
                new String[]{String.valueOf(teacherId)});
    }
}