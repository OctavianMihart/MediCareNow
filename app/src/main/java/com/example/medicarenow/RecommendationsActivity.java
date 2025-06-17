package com.example.medicarenow;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RecommendationsActivity extends AppCompatActivity {

    private TextView recommendationsText;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendations);

        recommendationsText = findViewById(R.id.recommendationsText);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseRef = database.getReference("rec");

        loadRecommendations();
    }

    private void loadRecommendations() {
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String recommendations = snapshot.child("aa").getValue(String.class);
                    recommendationsText.setText(recommendations);
                } else {
                    showLocalRecommendations();
                    Toast.makeText(RecommendationsActivity.this,
                            "Nu s-au găsit recomandări online", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showLocalRecommendations();
                Toast.makeText(RecommendationsActivity.this,
                        "Eroare la conectare: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLocalRecommendations() {
        String localRecommendations =
                "Recomandări medicale:\n\n" +
                        "1. 30 de minute de mișcare zilnic\n" +
                        "2. Dietă echilibrată cu reducere de sare\n" +
                        "3. Minim 2 litri de apă pe zi\n" +
                        "4. 7-8 ore de somn pe noapte\n" +
                        "5. Măsurare regulată a tensiunii\n" +
                        "6. Evitare stres\n" +
                        "7. Control medical lunar";
        recommendationsText.setText(localRecommendations);
    }
}