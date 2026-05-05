package com.example.attendancetrackerapp;

import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    // IMPORTANT: Must start with http:// and end with /
    String BASE_URL = "http://attendance-tracker.infinityfreeapp.com/attendance_api/";
    @FormUrlEncoded
    @POST("register.php")
    Call<ResponseBody> registerUser(
            @Field("name") String name,
            @Field("email") String email,
            @Field("password") String password
    );

    @FormUrlEncoded
    @POST("login.php")
    Call<ResponseBody> loginUser(
            @Field("email") String email,
            @Field("password") String password
    );

    @GET("manage_classes.php?action=get")
    Call<List<ClassModel>> getClasses(@Query("teacher_id") int teacherId);

    @FormUrlEncoded
    @POST("manage_classes.php?action=add")
    Call<ResponseBody> addClass(
            @Field("subject_name") String subjectName,
            @Field("section") String section,
            @Field("teacher_id") int teacherId
    );

    @GET("manage_students.php?action=get")
    Call<List<StudentModel>> getStudents(@Query("class_id") int classId);

    @FormUrlEncoded
    @POST("manage_students.php?action=add")
    Call<ResponseBody> addStudent(
            @Field("name") String name,
            @Field("student_number") String studentNumber,
            @Field("class_id") int classId
    );

    @FormUrlEncoded
    @POST("mark_attendance.php")
    Call<ResponseBody> markAttendance(
            @Field("student_id") int studentId,
            @Field("student_name") String studentName,
            @Field("class_name") String className,
            @Field("teacher_id") int teacherId,
            @Field("teacher_name") String teacherName,
            @Field("date") String date,
            @Field("status") String status
    );

    @GET("get_summary.php")
    Call<SummaryModel> getSummary(@Query("teacher_id") int teacherId, @Query("date") String date);

    @GET("get_reports.php")
    Call<List<ReportModel>> getReports(@Query("teacher_id") int teacherId);

    @FormUrlEncoded
    @POST("delete_account.php")
    Call<ResponseBody> deleteAccount(@Field("user_id") int userId);

    @FormUrlEncoded
    @POST("delete_class.php")
    Call<ResponseBody> deleteClass(@Field("class_id") int classId);

    @FormUrlEncoded
    @POST("delete_student.php")
    Call<ResponseBody> deleteStudent(@Field("student_id") int studentId);

    @FormUrlEncoded
    @POST("update_profile.php")
    Call<ResponseBody> updateProfile(
            @Field("user_id") int userId,
            @Field("name") String name,
            @Field("password") String password
    );

    static ApiService create() {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    okhttp3.Request request = chain.request().newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
                            .header("Accept", "application/json")
                            .build();
                    return chain.proceed(request);
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(ApiService.class);
    }

    // Models for JSON mapping
    class ClassModel {
        int id;
        String subject_name;
        String section;
    }

    class StudentModel {
        int id;
        String name;
        String student_number;
    }

    class SummaryModel {
        int present;
        int absent;
        int late;
        List<AtRiskModel> at_risk;
    }

    class AtRiskModel {
        String name;
        String student_number;
        int rate;
    }

    class ReportModel {
        String name;
        String student_number;
        int present_count;
        int total_count;
    }
}
