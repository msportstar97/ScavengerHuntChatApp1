package com.example.michigiang.scavengerhuntchatapp1;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = firebaseDatabase.getReference("Shovel-Armed Assassins/messages/-KisSptM3tSBhkh7K2WD");
    DatabaseReference newDatabaseRef = firebaseDatabase.getReference("child_rooms/friend");

    int numberOfChildren;

    @Test
    public void message() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        numberOfChildren = 0;

        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                numberOfChildren++;
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
        });

        countDownLatch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        assertEquals(2, numberOfChildren);
        assertTrue(countDownLatch.getCount() >= 0);
    }

    @Test
    public void writeTest() throws Exception {
        final CountDownLatch writeSignal = new CountDownLatch(1);

        newDatabaseRef.setValue("name").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                writeSignal.countDown();
            }
        });

        newDatabaseRef.child("name").setValue("MOFOS");

        writeSignal.await(10, java.util.concurrent.TimeUnit.SECONDS);
        assertEquals(0, writeSignal.getCount());
    }

    @Test
    public void readTest() throws Exception {
        final CountDownLatch writeSignal = new CountDownLatch(1);

        newDatabaseRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                assertEquals("MOFOS", dataSnapshot.getValue(String.class));
                writeSignal.countDown();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        writeSignal.await(10, java.util.concurrent.TimeUnit.SECONDS);
        assertEquals(0, writeSignal.getCount());
    }

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.example.michigiang.scavengerhuntchatapp1", appContext.getPackageName());
    }
}
