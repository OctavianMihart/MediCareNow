package com.example.medicarenow;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RecommendationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendations);

        TextView recommendationsText = findViewById(R.id.recommendationsText);

        String recommendations =
                "Recomandări medicale:\n\n" +
                        "1. 30 de minute de mișcare zilnic\n" +
                        "2. Dietă echilibrată cu reducere de sare\n" +
                        "3. Minim 2 litri de apă pe zi\n" +
                        "4. 7-8 ore de somn pe noapte\n" +
                        "5. Măsurare regulată a tensiunii\n" +
                        "6. Evitare stres\n" +
                        "7. Control medical lunar";

        recommendationsText.setText(recommendations);
    }
}