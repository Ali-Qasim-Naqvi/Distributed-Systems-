package be.kuleuven.foodrestservice.controllers;

import be.kuleuven.foodrestservice.domain.Meal;
import be.kuleuven.foodrestservice.domain.MealsRepository;
import be.kuleuven.foodrestservice.exceptions.MealNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Optional;

import static ch.qos.logback.core.joran.spi.ConsoleTarget.SystemOut;

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
    void addMeal(@RequestParam String name, @RequestParam String description,@RequestParam String mealType, @RequestParam int kcal, @RequestParam double price ){
        mealsRepository.addNewMeal(name,description,mealType,kcal,price);
    }

    @PutMapping("/restrpc/meals/updatemeal")
    void updateMeal(@RequestBody Meal meal){
        mealsRepository.updateExistingMeal(meal);
    }

    @DeleteMapping("/restrpc/meals/deletemeal")
    void deleteMeal(@RequestParam String id){
        System.out.println("Delete called");
        mealsRepository.deleteExistingMeal(id);
    }
}
