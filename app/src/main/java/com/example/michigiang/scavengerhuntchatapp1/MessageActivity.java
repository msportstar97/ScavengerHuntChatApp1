package com.example.michigiang.scavengerhuntchatapp1;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Michelle Giang giang2
 */

public class MessageActivity extends AppCompatActivity {
    //Firebase instance variables
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mChatsDatabaseReference;
    private ListView mchatListView;
    private ChatAdapter mChatAdapter;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private ChildEventListener mChildEventListener;

    public static final String ANONYMOUS = "anonymous";
    public static final int RC_SIGN_IN = 1;
    private String mUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        mUsername = ANONYMOUS;

        //Initialize firebase components and list view
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mChatsDatabaseReference = mFirebaseDatabase.getReference("child_rooms");
        mchatListView = (ListView) findViewById(R.id.chatListView);
        mFirebaseAuth = FirebaseAuth.getInstance();

        // Initialize chat ListView and its adapter
        List<ChatRoom> chatRooms = new ArrayList<>();
        mChatAdapter = new ChatAdapter(this, R.layout.chat_message, chatRooms);
        mchatListView.setAdapter(mChatAdapter);
        mchatListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ChatRoom room = (ChatRoom) adapterView.getItemAtPosition(i);
                //Launches the game and passes in the username and the chat room name
                Intent launchActivity = new Intent(MessageActivity.this, MainActivity.class);
                launchActivity.putExtra("username", mUsername);
                launchActivity.putExtra("room", room.getName());
                startActivity(launchActivity);
            }
        });

        //Check if user is logged in or not
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //user is signed in
                    onSignedInInitialize(user.getDisplayName());
                } else {
                    //user is signed out
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
    }

    //Attach authstatelistener
    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    //Remove authstatelistener
    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
    }

    //Handles all of the actions
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            /* Sign in succeeded, so the UI is created,
             *else if user cancels, the activity is finished
             *else if the user chooses the photo picker, it will upload the image onto firebase storage
            */
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "You are signed in", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Sign in cancelled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    //Makes sure the username shows so people know who's texting
    private void onSignedInInitialize(String username) {
        mUsername = username;
        attachDatabaseReadListener();
    }

    //Clears up the chat room screen
    private void onSignedOutCleanup() {
        mUsername = ANONYMOUS;
        mChatAdapter.clear();
    }

    //Creates the settings menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    //Signs out if sign out from the settings menu is selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_chat_room:
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setMessage("What is the name of the chat room?");
                final EditText input = new EditText(this);
                alert.setView(input);

                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String inputName = input.getText().toString();
                        ChatRoom chatRoom = new ChatRoom(inputName);
                        mChatsDatabaseReference.push().setValue(chatRoom);
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                AlertDialog build = alert.create();
                build.show();

                Button posButton = build.getButton(DialogInterface.BUTTON_POSITIVE);
                posButton.setTextColor(Color.BLUE);
                Button negButton = build.getButton(DialogInterface.BUTTON_NEGATIVE);
                negButton.setTextColor(Color.BLUE);
                return true;
            //sign out
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //Adds chat rooms to the database
    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    ChatRoom chatRoom = dataSnapshot.getValue(ChatRoom.class);
                    mChatAdapter.add(chatRoom);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };
            mChatsDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    //Removes the database listener
    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mChatsDatabaseReference.removeEventListener(mChildEventListener);
        }
    }
}
