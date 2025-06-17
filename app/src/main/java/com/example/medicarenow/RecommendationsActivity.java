package com.example.medicarenow;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecommendationsActivity extends AppCompatActivity {

    private TextView recommendationsText;
    private FirebaseFirestore db;
    private String currentUserId;
    private static final String TAG = "RecommendationsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting RecommendationsActivity");
        setContentView(R.layout.activity_recommendations);

        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "onCreate: Firestore instance initialized");

        // Check user session
        SharedPreferences prefs = getSharedPreferences("MediCareNow", MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", "");
        String userEmail = prefs.getString("user_email", "");

        Log.d(TAG, "onCreate: Current user ID: " + currentUserId + ", email: " + userEmail);

        if (currentUserId.isEmpty()) {
            Log.w(TAG, "onCreate: No user session found, redirecting to login");
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(RecommendationsActivity.this, LoginActivity.class));
            finish();
            return;
        }

        recommendationsText = findViewById(R.id.recommendationsText);

        loadMedicalRecommendations();
    }

    private void loadMedicalRecommendations() {
        Log.d(TAG, "loadMedicalRecommendations: Loading recommendations for user: " + currentUserId);
        recommendationsText.setText("Se încarcă recomandările medicale...");

        // Query Firestore for recommendations assigned to current user
        db.collection("recomandari")
                .whereEqualTo("pacientID", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "loadMedicalRecommendations: Query successful, found " + task.getResult().size()
                                + " recommendations");

                        if (task.getResult().isEmpty()) {
                            Log.w(TAG, "loadMedicalRecommendations: No recommendations found for user");
                            showLocalRecommendations();
                            return;
                        }

                        StringBuilder recommendationsContent = new StringBuilder();
                        recommendationsContent.append("🏥 RECOMANDĂRILE TALE MEDICALE\n");
                        recommendationsContent.append("═══════════════════════════════════════\n\n");

                        int activeCount = 0;
                        int completedCount = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, "loadMedicalRecommendations: Processing recommendation: " + document.getId());

                            String descriere = document.getString("descriere");
                            String tipRecomandare = document.getString("tipRecomandare");
                            String status = document.getString("status");
                            String medicID = document.getString("medicID");
                            Long progres = document.getLong("progres");
                            Object dataCreare = document.get("dataCreare");

                            if ("active".equals(status)) {
                                activeCount++;
                            } else {
                                completedCount++;
                            }

                            // Format the recommendation display
                            recommendationsContent.append("📋 RECOMANDAREA #").append(activeCount + completedCount)
                                    .append("\n");
                            recommendationsContent.append("───────────────────────────────────────\n");

                            if (descriere != null) {
                                recommendationsContent.append("💡 Descriere: ").append(descriere).append("\n");
                            }

                            if (tipRecomandare != null) {
                                String tipFormatted = formatRecommendationType(tipRecomandare);
                                recommendationsContent.append("🏷️  Tip: ").append(tipFormatted).append("\n");
                            }

                            if (status != null) {
                                String statusIcon = "active".equals(status) ? "🟢" : "✅";
                                String statusText = "active".equals(status) ? "ACTIVĂ" : "COMPLETATĂ";
                                recommendationsContent.append(statusIcon).append(" Status: ").append(statusText)
                                        .append("\n");
                            }

                            if (progres != null) {
                                recommendationsContent.append("📊 Progres: ").append(progres).append("%\n");

                                // Add progress bar
                                int progressBars = (int) (progres / 10);
                                StringBuilder progressBar = new StringBuilder("🔋 [");
                                for (int i = 0; i < 10; i++) {
                                    if (i < progressBars) {
                                        progressBar.append("█");
                                    } else {
                                        progressBar.append("░");
                                    }
                                }
                                progressBar.append("]\n");
                                recommendationsContent.append(progressBar);
                            }

                            if (medicID != null) {
                                recommendationsContent.append("👨‍⚕️ Medic ID: ").append(medicID).append("\n");
                            }

                            if (dataCreare != null) {
                                try {
                                    // Handle Firestore Timestamp
                                    String dateString = dataCreare.toString();
                                    recommendationsContent.append("📅 Data creării: ").append(dateString).append("\n");
                                } catch (Exception e) {
                                    Log.w(TAG, "Error formatting date: " + e.getMessage());
                                }
                            }

                            recommendationsContent.append("\n");
                        }

                        // Add summary
                        recommendationsContent.append("═══════════════════════════════════════\n");
                        recommendationsContent.append("📈 SUMAR RECOMANDĂRI\n");
                        recommendationsContent.append("═══════════════════════════════════════\n");
                        recommendationsContent.append("🟢 Active: ").append(activeCount).append("\n");
                        recommendationsContent.append("✅ Completate: ").append(completedCount).append("\n");
                        recommendationsContent.append("📊 Total: ").append(activeCount + completedCount).append("\n\n");

                        // Add general health tips
                        recommendationsContent.append("💡 SFATURI GENERALE DE SĂNĂTATE\n");
                        recommendationsContent.append("═══════════════════════════════════════\n");
                        recommendationsContent.append("• Urmați cu atenție recomandările medicale\n");
                        recommendationsContent.append("• Băți minim 2 litri de apă pe zi\n");
                        recommendationsContent.append("• Dormiți 7-8 ore pe noapte\n");
                        recommendationsContent.append("• Exerciții fizice regulate\n");
                        recommendationsContent.append("• Evitați stresul\n");
                        recommendationsContent.append("• Control medical periodic\n");

                        recommendationsText.setText(recommendationsContent.toString());
                        Log.d(TAG, "loadMedicalRecommendations: Recommendations displayed successfully");

                    } else {
                        Log.e(TAG, "loadMedicalRecommendations: Error getting recommendations", task.getException());
                        showLocalRecommendations();
                        Toast.makeText(RecommendationsActivity.this,
                                "Eroare la încărcarea recomandărilor: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String formatRecommendationType(String tip) {
        switch (tip.toLowerCase()) {
            case "stil-viata":
                return "Stil de viață";
            case "medicatie":
                return "Medicație";
            case "exercitii":
                return "Exerciții";
            case "dieta":
                return "Dietă";
            case "control":
                return "Control medical";
            default:
                return tip;
        }
    }

    private void showLocalRecommendations() {
        Log.d(TAG, "showLocalRecommendations: Showing default recommendations");
        String localRecommendations = "🏥 RECOMANDĂRI MEDICALE GENERALE\n" +
                "═══════════════════════════════════════\n\n" +
                "📋 Nu s-au găsit recomandări personalizate.\n\n" +
                "💡 SFATURI GENERALE DE SĂNĂTATE:\n" +
                "───────────────────────────────────────\n" +
                "1. 🚶 30 de minute de mișcare zilnic\n" +
                "2. 🥗 Dietă echilibrată cu reducere de sare\n" +
                "3. 💧 Minim 2 litri de apă pe zi\n" +
                "4. 😴 7-8 ore de somn pe noapte\n" +
                "5. 🩺 Măsurare regulată a tensiunii\n" +
                "6. 😌 Evitare stres\n" +
                "7. 👨‍⚕️ Control medical lunar\n\n" +
                "═══════════════════════════════════════\n" +
                "📞 Contactați medicul pentru recomandări personalizate.";
        recommendationsText.setText(localRecommendations);
    }
}