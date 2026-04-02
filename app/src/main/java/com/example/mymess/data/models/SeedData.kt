package com.example.mymess.data.models

object SeedData {
    val defaultCloudMeals: List<Meal> = listOf(
        Meal(
            mealId = "seed-cloud-1",
            messId = "seed-mess-1",
            name = "Veg Thali",
            description = "2 roti, rice, dal, sabzi",
            price = 90.0,
            type = "cloud",
            mealSection = "cloud",
            isAvailable = true,
        ),
        Meal(
            mealId = "seed-cloud-2",
            messId = "seed-mess-1",
            name = "Paneer Combo",
            description = "Paneer curry, jeera rice, salad",
            price = 140.0,
            type = "cloud",
            mealSection = "cloud",
            isAvailable = true,
        ),
        Meal(
            mealId = "seed-cloud-3",
            messId = "seed-mess-2",
            name = "South Meal",
            description = "Sambar rice, curd rice, papad",
            price = 110.0,
            type = "cloud",
            mealSection = "cloud",
            isAvailable = true,
        ),
    )
}

