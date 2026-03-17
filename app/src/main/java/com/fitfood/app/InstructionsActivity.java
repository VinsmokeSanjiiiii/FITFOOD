package com.fitfood.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;

public class InstructionsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instructions);

        ImageButton btnBack = findViewById(R.id.btnBack);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.rvInstructions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadUserInstructions();

        btnBack.setOnClickListener(v -> finish());
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    loadUserInstructions();
                } else {
                    loadAdminInstructions();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadUserInstructions() {
        List<InstructionStep> steps = new ArrayList<>();

        steps.add(new InstructionStep("1. Create an Account / Login", "Register using Email or Google. Confirm your Google account if prompted.", R.drawable.step_login2));
        steps.add(new InstructionStep("2. Choose Your Goal", "Select Weight Loss or Weight Gain to adjust your calorie recommendation.", R.drawable.step_goal));
        steps.add(new InstructionStep("3. Check Health Risks", "Read about obesity-related diseases and tips to stay aware.", R.drawable.step_risks));
        steps.add(new InstructionStep("4. View the Food Guide", "Check recommended foods and items to limit for better choices.", R.drawable.step_guide));
        steps.add(new InstructionStep("5. Use the Calorie Calculator", "Enter age, weight, and height to compute daily needs.", R.drawable.step_calc));
        steps.add(new InstructionStep("6. Log Your Meals", "Select Breakfast, Lunch, Dinner, or Snacks and add food items.", R.drawable.step_log));
        steps.add(new InstructionStep("7. Track Your Progress", "View daily trends, remaining calories, and weight changes.", R.drawable.step_track));
        steps.add(new InstructionStep("8. Admin Monitoring", "Admins monitor data to provide guidance and support.", R.drawable.step_admin_info));

        recyclerView.setAdapter(new InstructionAdapter(steps));
    }

    private void loadAdminInstructions() {
        List<InstructionStep> steps = new ArrayList<>();

        steps.add(new InstructionStep("1. Welcome Admin", "Admin accounts are created by existing admins. Login with your credentials.", R.drawable.step_login));
        steps.add(new InstructionStep("2. Dashboard & Overview", "View total registered users and monitor engagement.", R.drawable.admin_dash));
        steps.add(new InstructionStep("3. Progress & Meal Logs", "Check weight trends and review meal logs for compliance.", R.drawable.admin_logs));
        steps.add(new InstructionStep("4. Food Library Management", "Add, edit, or remove food items to keep the database accurate.", R.drawable.admin_library));
        steps.add(new InstructionStep("5. User Management", "Create new admins and send messages to users needing support.", R.drawable.admin_users));

        recyclerView.setAdapter(new InstructionAdapter(steps));
    }

    static class InstructionStep {
        String title;
        String description;
        int imageRes;

        public InstructionStep(String title, String description, int imageRes) {
            this.title = title;
            this.description = description;
            this.imageRes = imageRes;
        }
    }

    private class InstructionAdapter extends RecyclerView.Adapter<InstructionAdapter.ViewHolder> {
        private final List<InstructionStep> steps;

        public InstructionAdapter(List<InstructionStep> steps) {
            this.steps = steps;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_instruction, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            InstructionStep step = steps.get(position);
            holder.tvTitle.setText(step.title);
            holder.tvDesc.setText(step.description);
            holder.ivImage.setImageResource(step.imageRes);
        }

        @Override
        public int getItemCount() {
            return steps.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDesc;
            ImageView ivImage;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvStepTitle);
                tvDesc = itemView.findViewById(R.id.tvStepDescription);
                ivImage = itemView.findViewById(R.id.ivStepImage);
            }
        }
    }
}