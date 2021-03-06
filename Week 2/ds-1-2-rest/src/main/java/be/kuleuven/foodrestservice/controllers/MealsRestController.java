package be.kuleuven.foodrestservice.controllers;

import be.kuleuven.foodrestservice.domain.Meal;
import be.kuleuven.foodrestservice.domain.MealsRepository;
import be.kuleuven.foodrestservice.exceptions.MealNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
public class MealsRestController {

    private final MealsRepository mealsRepository;

    @Autowired
    MealsRestController(MealsRepository mealsRepository) {
        this.mealsRepository = mealsRepository;
    }

    @GetMapping("/rest/meals/{id}")
    EntityModel<Meal> getMealById(@PathVariable String id) {
        Meal meal = mealsRepository.findMeal(id).orElseThrow(() -> new MealNotFoundException(id));

        return mealToEntityModel(id, meal);
    }

    @GetMapping("/rest/meals/biggestmeal")
    EntityModel<Meal> getBiggestMeal() {
        Meal meal = mealsRepository.findBiggestMeal().orElseThrow(() -> new MealNotFoundException("No biggest meal"));

        return mealToEntityModel(meal.getId(), meal);
    }

    @GetMapping("/rest/meals/cheapestmeal")
    EntityModel<Meal> getCheapestMeal() {
        Meal meal = mealsRepository.findCheapestMeal().orElseThrow(() -> new MealNotFoundException("No cheapest meal"));

        return mealToEntityModel(meal.getId(), meal);
    }

    @PostMapping("/rest/meals/addmeal")
    void addMeal(@RequestParam String name, @RequestParam String description,@RequestParam String mealType, @RequestParam int kcal, @RequestParam double price ){
        mealsRepository.addNewMeal(name,description,mealType,kcal,price);
    }

    @PostMapping("/rest/meals/updatemeal")
    void updateMeal(@RequestBody Meal meal){
        System.out.println("Updating Meal");
        mealsRepository.updateExistingMeal(meal);
    }

    @DeleteMapping("/rest/meals/deletemeal")
    void deleteMeal(@RequestParam String id){
        System.out.println("Delete called");
        mealsRepository.deleteExistingMeal(id);
    }

    @GetMapping("/rest/meals")
    CollectionModel<EntityModel<Meal>> getMeals() {
        Collection<Meal> meals = mealsRepository.getAllMeal();

        List<EntityModel<Meal>> mealEntityModels = new ArrayList<>();
        for (Meal m : meals) {
            EntityModel<Meal> em = mealToEntityModel(m.getId(), m);
            mealEntityModels.add(em);
        }
        return CollectionModel.of(mealEntityModels,
                linkTo(methodOn(MealsRestController.class).getMeals()).withSelfRel());
    }

    private EntityModel<Meal> mealToEntityModel(String id, Meal meal) {
        return EntityModel.of(meal,
                linkTo(methodOn(MealsRestController.class).getMealById(id)).withSelfRel(),
                linkTo(methodOn(MealsRestController.class).getMeals()).withRel("rest/meals"));
    }

}
