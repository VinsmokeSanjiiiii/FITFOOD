package com.fitfood.app;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;

public class InstructionsActivity extends AppCompatActivity {

    private TextView tvContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instructions);
        ImageButton btnBack = findViewById(R.id.btnBack);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tvContent = findViewById(R.id.tvInstructionContent);
        showUserInstructions();
        btnBack.setOnClickListener(v -> finish());
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showUserInstructions();
                } else {
                    showAdminInstructions();
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showUserInstructions() {
        String html = "<b>1. Create an Account / Login</b><br/>" +
                "Register using Email or Google. Confirm your Google account if prompted.<br/><br/>" +

                "<b>2. Choose Your Goal</b><br/>" +
                "Select <b>Weight Loss</b> or <b>Weight Gain</b> to adjust your calorie recommendation.<br/><br/>" +

                "<b>3. Check Health Risks</b><br/>" +
                "Read about obesity-related diseases and tips to stay aware.<br/><br/>" +

                "<b>4. View the Food Guide</b><br/>" +
                "Check recommended foods and items to limit for better choices.<br/><br/>" +

                "<b>5. Use the Calorie Calculator</b><br/>" +
                "Enter age, weight, and height to compute daily needs. <i>Do this before logging meals.</i><br/><br/>" +

                "<b>6. Log Your Meals</b><br/>" +
                "Select Breakfast, Lunch, Dinner, or Snacks and add food items.<br/><br/>" +

                "<b>7. Track Your Progress</b><br/>" +
                "View daily trends, remaining calories, and weight changes.<br/><br/>" +

                "<b>8. Admin Monitoring</b><br/>" +
                "Admins monitor data to provide guidance and support.<br/><br/>" +

                "<i>Repeat daily for a healthier lifestyle!</i>";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvContent.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT));
        }
    }

    private void showAdminInstructions() {
        String html = "<b>Welcome Admin</b><br/>" +
                "1. Admin accounts are created by existing admins.<br/>" +
                "2. Login with your admin credentials.<br/><br/>" +

                "<b>2. Dashboard & User Overview</b><br/>" +
                "View total registered users and monitor engagement.<br/><br/>" +

                "<b>3. User Progress & Meal Logs</b><br/>" +
                "Check weight trends and review meal logs for compliance.<br/><br/>" +

                "<b>4. Food Library Management</b><br/>" +
                "Add, edit, or remove food items to keep the database accurate.<br/><br/>" +

                "<b>5. User Management</b><br/>" +
                "Create new admins, edit user details, and <b>send messages</b> to users needing support.<br/><br/>" +

                "<b>6. Monitoring</b><br/>" +
                "Actively monitor activity and guide users who are off-track.";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvContent.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT));
        }
    }
}