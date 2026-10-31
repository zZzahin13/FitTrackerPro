package com.example.fittrackerpro.utils;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public class AuthMessages {

    public static String from(Exception e) {
        if (e instanceof FirebaseNetworkException) {
            return "No internet connection. Check your network and try again.";
        }
        // WeakPassword must be checked before InvalidCredentials (it is a subclass)
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "Password is too weak. Use at least 6 characters.";
        }
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "An account with this email already exists.";
        }
        if (e instanceof FirebaseAuthInvalidUserException
                || e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Incorrect email or password.";
        }
        if (e instanceof FirebaseTooManyRequestsException) {
            return "Too many attempts. Please wait a moment and try again.";
        }
        return "Something went wrong. Please try again.";
    }
}