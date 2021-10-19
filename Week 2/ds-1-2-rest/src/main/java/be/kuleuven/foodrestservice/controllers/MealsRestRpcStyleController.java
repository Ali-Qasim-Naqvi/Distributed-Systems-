package be.kuleuven.foodrestservice.controllers;

import be.kuleuven.foodrestservice.domain.Meal;
import be.kuleuven.foodrestservice.domain.MealsRepository;
import be.kuleuven.foodrestservice.exceptions.MealNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Optional;

@RestController
public class MealsRestRpcStyleController {

    private final MealsRepository mealsRepository;

    @Autowired
    MealsRestRpcStyleController(MealsRepository mealsRepository) {
        this.mealsRepository = mealsRepository;
    }

    @GetMapping("/restrpc/meals/{id}")
    Meal getMealById(@PathVariable String id) {
        Optional<Meal> meal = mealsRepository.findMeal(id);

        return meal.orElseThrow(() -> new MealNotFoundException(id));
    }

    @GetMapping("/restrpc/meals")
    Collection<Meal> getMeals() {
        return mealsRepository.getAllMeal();
    }

    @GetMapping("/restrpc/meals/biggestmeal")
    Meal getBiggestMeal() {
        Optional<Meal> meal = mealsRepository.findBiggestMeal();

        return meal.orElseThrow(() -> new MealNotFoundException("No biggest meal"));
    }

    @GetMapping("/restrpc/meals/cheapestmeal")
    Meal getCheapestMeal() {
        Optional<Meal> meal = mealsRepository.findCheapestMeal();

        return meal.orElseThrow(() -> new MealNotFoundException("No cheapest meal"));
    }

    @PostMapping("/restrpc/meals/addmeal")
    void addMeal(@RequestBody String name, @RequestBody String description,@RequestBody String mealType, @RequestBody int kcal, @RequestBody double price ){
        mealsRepository.addNewMeal(name,description,mealType,kcal,price);
    }

    @PutMapping("/restrpc/meals/updatemeal")
    void updateMeal(@RequestBody String id, @RequestBody String name, @RequestBody String description,@RequestBody String mealType, @RequestBody int kcal, @RequestBody double price ){
        mealsRepository.updateExistingMeal(id,name,description,mealType,kcal,price);
    }

    @DeleteMapping("/restrpc/meals/deletemeal")
    void deleteMeal(@RequestBody String id){
        mealsRepository.deleteExistingMeal(id);
    }
}
