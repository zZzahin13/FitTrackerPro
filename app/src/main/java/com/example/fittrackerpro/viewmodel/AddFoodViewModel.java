package com.example.fittrackerpro.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.fittrackerpro.database.entity.Meal;
import com.example.fittrackerpro.repository.NutritionRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class AddFoodViewModel extends AndroidViewModel {

    private final NutritionRepository repository;
    private final String userId;
    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final LiveData<List<Meal>> foods;

    public AddFoodViewModel(@NonNull Application application) {
        super(application);
        repository = new NutritionRepository(application);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : "";

        repository.ensureSeeded();
        foods = Transformations.switchMap(query, q -> repository.searchFoods(q));
    }

    public LiveData<List<Meal>> getFoods() {
        return foods;
    }

    public void setQuery(String text) {
        query.setValue(text);
    }

    public void addLog(Meal food, double quantity, String mealType, long dayStart) {
        repository.addLog(userId, food, quantity, mealType, dayStart);
    }

    public void addCustomFood(Meal meal) {
        repository.addCustomFood(meal, null);
    }
}