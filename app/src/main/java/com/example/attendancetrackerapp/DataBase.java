package com.example.attendancetrackerapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DataBase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "attendance.db";
    private static final int DATABASE_VERSION = 1;

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
                "role TEXT DEFAULT 'instructor'," +
                "profile_pic TEXT," +
                "expected_start TEXT DEFAULT '08:00'," +
                "expected_end TEXT DEFAULT '17:00'" +
                ")");

        db.execSQL("CREATE TABLE classes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "subject_name TEXT NOT NULL," +
                "section TEXT NOT NULL," +
                "instructor_id INTEGER NOT NULL," +
                "FOREIGN KEY (instructor_id) REFERENCES users(id)" +
                ")");

        db.execSQL("CREATE TABLE students (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "student_number TEXT NOT NULL," +
                "name TEXT NOT NULL," +
                "class_id INTEGER NOT NULL," +
                "FOREIGN KEY (class_id) REFERENCES classes(id)" +
                ")");

        db.execSQL("CREATE TABLE student_attendance (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "student_id INTEGER NOT NULL," +
                "date TEXT NOT NULL," +
                "status TEXT NOT NULL," +
                "FOREIGN KEY (student_id) REFERENCES students(id)" +
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
        db.execSQL("DROP TABLE IF EXISTS student_attendance");
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
        ContentValues values = new ContentValues();
        values.put("clock_out", time);
        int rows = db.update("teacher_attendance", values,
                "teacher_id=? AND date=?",
                new String[]{String.valueOf(teacherId), date});
        db.close();
        return rows > 0;
    }

    public Cursor getTeacherAttendanceToday(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT u.id, u.name, u.email, u.expected_start, u.expected_end, " +
                        "ta.clock_in, ta.clock_out, ta.status, ta.id as record_id " +
                        "FROM users u " +
                        "LEFT JOIN teacher_attendance ta ON u.id = ta.teacher_id " +
                        "AND ta.date=? " +
                        "WHERE u.role='instructor' ORDER BY u.name ASC",
                new String[]{date});
    }

    public boolean adminMarkTeacher(int teacherId, String date, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor check = db.rawQuery(
                "SELECT id FROM teacher_attendance WHERE teacher_id=? AND date=?",
                new String[]{String.valueOf(teacherId), date});
        if (check.getCount() > 0) {
            check.close();
            ContentValues values = new ContentValues();
            values.put("status", status);
            int rows = db.update("teacher_attendance", values,
                    "teacher_id=? AND date=?",
                    new String[]{String.valueOf(teacherId), date});
            db.close();
            return rows > 0;
        }
        check.close();
        ContentValues values = new ContentValues();
        values.put("teacher_id", teacherId);
        values.put("date", date);
        values.put("status", status);
        db.insert("teacher_attendance", null, values);
        db.close();
        return true;
    }

    public Cursor getTeacherMonthlySummary(int teacherId, String yearMonth) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT status, COUNT(*) as count " +
                        "FROM teacher_attendance " +
                        "WHERE teacher_id=? AND date LIKE ? " +
                        "GROUP BY status",
                new String[]{String.valueOf(teacherId), yearMonth + "%"});
    }

    public Cursor getAllTeacherStats(String yearMonth) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT u.id, u.name, " +
                        "COUNT(CASE WHEN ta.status='present' THEN 1 END) as on_time, " +
                        "COUNT(CASE WHEN ta.status='late' THEN 1 END) as late_count, " +
                        "COUNT(ta.id) as total " +
                        "FROM users u " +
                        "LEFT JOIN teacher_attendance ta ON u.id = ta.teacher_id " +
                        "AND ta.date LIKE ? " +
                        "WHERE u.role='instructor' " +
                        "GROUP BY u.id ORDER BY on_time DESC",
                new String[]{yearMonth + "%"});
    }

    public boolean updateTeacherSchedule(int teacherId,
                                         String expectedStart, String expectedEnd) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("expected_start", expectedStart);
        values.put("expected_end", expectedEnd);
        int rows = db.update("users", values, "id=?",
                new String[]{String.valueOf(teacherId)});
        db.close();
        return rows > 0;
    }

    public Cursor getAllTeachers() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM users WHERE role='instructor' ORDER BY name ASC",
                null);
    }

    public Cursor getTeacherRecordForToday(int teacherId, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM teacher_attendance WHERE teacher_id=? AND date=?",
                new String[]{String.valueOf(teacherId), date});
    }

    // --- USER METHODS ---

    public boolean registerUser(String name, String email, String password, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("password", password);
        values.put("role", role);
        long result = db.insert("users", null, values);
        db.close();
        return result != -1;
    }

    public Cursor loginUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM users WHERE email=? AND password=?",
                new String[]{email, password});
    }

    // --- STUDENT METHODS ---

    public boolean addStudent(String studentNumber, String name, String section) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("student_number", studentNumber);
        values.put("name", name);
        values.put("section", section);
        long result = db.insert("students", null, values);
        db.close();
        return result != -1;
    }

    // --- SUBJECT/CLASS METHODS ---

    public boolean addClass(String subjectName, String section, int instructorId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("subject_name", subjectName);
        values.put("section", section);
        values.put("instructor_id", instructorId);
        long result = db.insert("classes", null, values);
        db.close();
        return result != -1;
    }

    public Cursor getClassesByInstructor(int instructorId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM classes WHERE instructor_id=?",
                new String[]{String.valueOf(instructorId)});
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

    public Cursor getAllStudents(String section) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM students WHERE section=? ORDER BY student_number ASC",
                new String[]{section});
    }

    public Cursor findStudentByNumber(String studentNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM students WHERE student_number=?",
                new String[]{studentNumber});
    }

    public boolean updateStudent(int id, String name, String section) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("section", section);
        int rows = db.update("students", values, "id=?", new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }

    public boolean deleteStudent(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete("students", "id=?", new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }

    // --- ATTENDANCE METHODS ---

    public boolean markAttendance(int studentId, String date, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("student_id", studentId);
        values.put("date", date);
        values.put("status", status);
        long result = db.insert("attendance", null, values);
        db.close();
        return result != -1;
    }

    public Cursor getAttendanceByDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT s.name, s.student_number, a.status " +
                        "FROM attendance a JOIN students s ON a.student_id = s.id " +
                        "WHERE a.date=? ORDER BY s.student_number ASC",
                new String[]{date});
    }

    public Cursor getAttendanceSummary() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT s.name, s.student_number, " +
                        "COUNT(CASE WHEN a.status='present' THEN 1 END) as present_count, " +
                        "COUNT(a.id) as total_count " +
                        "FROM students s LEFT JOIN attendance a ON s.id = a.student_id " +
                        "GROUP BY s.id ORDER BY s.student_number ASC",
                null);
    }

    public boolean updateAttendance(int id, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status);
        int rows = db.update("attendance", values, "id=?", new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }
}