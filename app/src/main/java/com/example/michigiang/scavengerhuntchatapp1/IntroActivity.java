package com.example.michigiang.scavengerhuntchatapp1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Michelle Giang giang2
 */

public class IntroActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        //Initializes the textview and buttons
        final TextView instructionsText = (TextView) findViewById(R.id.instructionsText);
        final Button continueButton = (Button) findViewById(R.id.continue_button);

        instructionsText.setMovementMethod(new ScrollingMovementMethod());

        //Displays the instructions for the game
        instructionsText.setText("Instructions: \n\t Choose a chat or create a chat if it's non-existent. " +
                "Whoever first enters the chat room becomes the leader. " +
                "Have the leader type the list of items/pics you will scavenge, " +
                "i.e. Picture with a CS professor, pic of a buggy, etc." +
                " You can chat with the other players while coming up with the list. " +
                "Once the list is finalized, have the leader click the finalize button. " +
                "This list should now appear on everyone's screen." +
                "This will also enable the time buttons. " +
                "When everyone is ready, the leader can start the timer of 30 minutes to play the game. " +
                "This timer will also appear on everyone's screen. " +
                "Once the time is up, the chat will be disabled so no one can chat any more pictures they scavenged. " +
                "You can meet up with the other players and determine who wins. " +
                "Whoever has pics of the most items on the list wins! \n" +
                "IMPORTANT: When you upload pictures, you will have to exit out of the chat room and re-enter it again" +
                "to see the messages and the images. However, if you are the leader and you exit the room," +
                "no one will be able to stop the game until the timer goes to 0, or unless everyone exits the game" +
                "and goes back in, but that also restarts the game. Therefore, it is HIGHLY suggested that everyone adds" +
                "their pictures near the end of the game.");

        //Continues on to the next activity, which is the chat room list
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent launchActivity = new Intent(IntroActivity.this, MessageActivity.class);
                startActivity(launchActivity);
            }
        });
    }
}
