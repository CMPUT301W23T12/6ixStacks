package com.example.qrrush;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    View mainView;

    Button profileButton;
    Button shopButton;
    Button mainButton;
    Button socialButton;
    Button leaderboardButton;

    User user;

    String username;

    private FirebaseFirestore firestore;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * Users infromation stored here hard coded values for now
         * untill firebase is setup
         * TODO Add firebase integration
         */

        ArrayList<QRCode> qrCodes = new ArrayList<>();

        byte[] b = new byte[20];
        Random rand = new Random();
        byte[][] sampleData = {
                new byte[20],
                new byte[20],
                new byte[20],
        };

        rand.nextBytes(sampleData[0]);
        rand.nextBytes(sampleData[1]);
        rand.nextBytes(sampleData[2]);

        qrCodes.add(new QRCode(sampleData[0]));
        qrCodes.add(new QRCode(sampleData[1]));
        qrCodes.add(new QRCode(sampleData[2]));

        // TODO: remove copyrighted material before merging into main.
        // Testing new commit into branch


        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        // Get Fire store instance
        firestore = FirebaseFirestore.getInstance();


        mainView = findViewById(R.id.main_view);
        profileButton = findViewById(R.id.profile_button);
        shopButton = findViewById(R.id.shop_button);
        socialButton = findViewById(R.id.social_button);
        mainButton = findViewById(R.id.main_button);
        leaderboardButton = findViewById(R.id.leaderboard_button);


        if (UserUtil.isFirstTimeLogin(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.first_time_launch_popup, null);
            EditText usernameEditText = dialogView.findViewById(R.id.username_input);
            EditText phoneNumberEditText = dialogView.findViewById(R.id.phone_number_input);
            TextView username_Taken = dialogView.findViewById(R.id.username_error);
            builder.setTitle("Enter your information");
            builder.setView(dialogView);

            // Check if the username is unique when the user clicks "OK" on the first time run
            // prompt.
            builder.setPositiveButton("OK", null);
            AlertDialog alertDialog = builder.create();
            alertDialog.setOnShowListener((dialog) -> {
                Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(v -> {
                    username = usernameEditText.getText().toString();
                    String phoneNumber = phoneNumberEditText.getText().toString();
                    FirebaseWrapper.checkUsernameAvailability(username, new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (!task.isSuccessful()) {
                                // Error occurred while querying database
                                Log.e("Firebase", "ERROR QUERYING DATABASE WHILE SEARCHING PROFILES COLLECTION");
                                return;
                            }

                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot.size() > 0) {
                                // Username is taken, prompt user to pick a new name
                                username_Taken.setVisibility(dialogView.VISIBLE);
                                return;
                            }

                            // Username is unique, continue with registration process
                            HashMap<String, Object> profiles = new HashMap<>();
                            profiles.put(username, UserUtil.generateUUID());
                            profiles.put("phone-number", phoneNumber);
                            profiles.put("rank", 0);
                            profiles.put("score", 0);
                            List<Map<String, Object>> qrCodeList = new ArrayList<>();
                            for (QRCode qrCode : qrCodes) {
                                Map<String, Object> qrCodeMap = qrCode.toMap();
                                qrCodeList.add(qrCodeMap);
                            }
                            profiles.put("qrcodes", qrCodeList);
                            // Add name + UUID and phonenumber to FB
                            FirebaseWrapper.addData("profiles", username, profiles);
                            // Create the user for them


                            // set firstTimeLogin to false
                            UserUtil.setFirstTime(MainActivity.this, true);
                            UserUtil.setUsername(MainActivity.this, username);
                            dialog.dismiss();
                        }
                    });
                });
            });
            alertDialog.show();
        }

        // Retrieve data from Firebase:
        Log.d("TAG", UserUtil.getUsername(MainActivity.this));
        FirebaseWrapper.getUserData(username, new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        user = new User(username, document.getString("phone-number"), document.getLong("rank").intValue(), document.getLong("score").intValue(), qrCodes);
                    }

                } else {
                    Log.d("Firebase User", "Error creating user");
                }
            }
        });


        profileButton.setOnClickListener((v) -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_view, new ProfileFragment(user)).commit();
        });

        shopButton.setOnClickListener((v) -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_view, new ShopFragment(user)).commit();
        });

        socialButton.setOnClickListener((v) -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_view, new SocialFragment(user)).commit();
        });

        mainButton.setOnClickListener((v) -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_view, new MainFragment(user)).commit();
        });

        leaderboardButton.setOnClickListener((v) -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_view, new LeaderboardFragment(user)).commit();
        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_view, new MainFragment(user)).commit();
    }
}