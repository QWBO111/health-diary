package com.healthdiary.app.data.local

object SeedData {
    suspend fun seedIfEmpty(db: AppDatabase) {
        if (db.exerciseLibraryDao().count() == 0) {
            db.exerciseLibraryDao().insertAll(EXERCISES)
        }
        if (db.foodLibraryDao().count() == 0) {
            db.foodLibraryDao().insertAll(FOODS)
        }
    }

    private val EXERCISES = listOf(
        ExerciseEntity(name = "杠铃卧推", muscleGroup = "胸"),
        ExerciseEntity(name = "上斜哑铃卧推", muscleGroup = "胸"),
        ExerciseEntity(name = "俯卧撑", muscleGroup = "胸"),
        ExerciseEntity(name = "双杠臂屈伸", muscleGroup = "胸"),
        ExerciseEntity(name = "引体向上", muscleGroup = "背"),
        ExerciseEntity(name = "高位下拉", muscleGroup = "背"),
        ExerciseEntity(name = "杠铃划船", muscleGroup = "背"),
        ExerciseEntity(name = "坐姿划船", muscleGroup = "背"),
        ExerciseEntity(name = "硬拉", muscleGroup = "背/腿"),
        ExerciseEntity(name = "杠铃深蹲", muscleGroup = "腿"),
        ExerciseEntity(name = "腿举", muscleGroup = "腿"),
        ExerciseEntity(name = "箭步蹲", muscleGroup = "腿"),
        ExerciseEntity(name = "腿弯举", muscleGroup = "腿"),
        ExerciseEntity(name = "腿屈伸", muscleGroup = "腿"),
        ExerciseEntity(name = "站姿提踵", muscleGroup = "小腿"),
        ExerciseEntity(name = "杠铃推举", muscleGroup = "肩"),
        ExerciseEntity(name = "哑铃侧平举", muscleGroup = "肩"),
        ExerciseEntity(name = "面拉", muscleGroup = "肩"),
        ExerciseEntity(name = "杠铃弯举", muscleGroup = "二头"),
        ExerciseEntity(name = "哑铃交替弯举", muscleGroup = "二头"),
        ExerciseEntity(name = "锤式弯举", muscleGroup = "前臂"),
        ExerciseEntity(name = "绳索下压", muscleGroup = "三头"),
        ExerciseEntity(name = "仰卧臂屈伸", muscleGroup = "三头"),
        ExerciseEntity(name = "卷腹", muscleGroup = "腹"),
        ExerciseEntity(name = "平板支撑", muscleGroup = "核心"),
        ExerciseEntity(name = "俄罗斯转体", muscleGroup = "腹"),
        ExerciseEntity(name = "悬垂举腿", muscleGroup = "腹"),
        ExerciseEntity(name = "跑步", muscleGroup = "有氧"),
        ExerciseEntity(name = "椭圆机", muscleGroup = "有氧"),
        ExerciseEntity(name = "跳绳", muscleGroup = "有氧"),
        ExerciseEntity(name = "游泳", muscleGroup = "有氧"),
        ExerciseEntity(name = "划船机", muscleGroup = "有氧"),
        ExerciseEntity(name = "动感单车", muscleGroup = "有氧")
    )

    private val FOODS = listOf(
        FoodEntity(name = "米饭", caloriesPer100g = 116f, proteinPer100g = 2.6f, carbsPer100g = 25.9f, fatPer100g = 0.3f, category = "主食"),
        FoodEntity(name = "白粥", caloriesPer100g = 46f, proteinPer100g = 1.1f, carbsPer100g = 9.9f, fatPer100g = 0.3f, category = "主食"),
        FoodEntity(name = "馒头", caloriesPer100g = 223f, proteinPer100g = 7.0f, carbsPer100g = 47.0f, fatPer100g = 1.1f, category = "主食"),
        FoodEntity(name = "煮面条", caloriesPer100g = 110f, proteinPer100g = 3.9f, carbsPer100g = 22.8f, fatPer100g = 0.4f, category = "主食"),
        FoodEntity(name = "全麦面包", caloriesPer100g = 246f, proteinPer100g = 10.0f, carbsPer100g = 41.0f, fatPer100g = 4.2f, category = "主食"),
        FoodEntity(name = "燕麦片", caloriesPer100g = 367f, proteinPer100g = 15.0f, carbsPer100g = 61.0f, fatPer100g = 6.7f, category = "主食"),
        FoodEntity(name = "鸡蛋", caloriesPer100g = 144f, proteinPer100g = 13.3f, carbsPer100g = 2.8f, fatPer100g = 8.8f, category = "蛋奶"),
        FoodEntity(name = "鸡胸肉", caloriesPer100g = 133f, proteinPer100g = 24.6f, carbsPer100g = 0.6f, fatPer100g = 5.0f, category = "肉蛋"),
        FoodEntity(name = "瘦牛肉", caloriesPer100g = 106f, proteinPer100g = 20.2f, carbsPer100g = 1.2f, fatPer100g = 2.3f, category = "肉蛋"),
        FoodEntity(name = "猪里脊", caloriesPer100g = 155f, proteinPer100g = 20.2f, carbsPer100g = 0.7f, fatPer100g = 7.9f, category = "肉蛋"),
        FoodEntity(name = "三文鱼", caloriesPer100g = 139f, proteinPer100g = 17.2f, carbsPer100g = 0f, fatPer100g = 7.8f, category = "肉蛋"),
        FoodEntity(name = "虾仁", caloriesPer100g = 93f, proteinPer100g = 18.6f, carbsPer100g = 2.8f, fatPer100g = 0.8f, category = "肉蛋"),
        FoodEntity(name = "牛奶", caloriesPer100g = 54f, proteinPer100g = 3.0f, carbsPer100g = 3.4f, fatPer100g = 3.2f, category = "蛋奶"),
        FoodEntity(name = "酸奶", caloriesPer100g = 72f, proteinPer100g = 2.5f, carbsPer100g = 9.3f, fatPer100g = 2.7f, category = "蛋奶"),
        FoodEntity(name = "豆腐", caloriesPer100g = 84f, proteinPer100g = 8.1f, carbsPer100g = 4.2f, fatPer100g = 3.7f, category = "豆制品"),
        FoodEntity(name = "豆浆", caloriesPer100g = 31f, proteinPer100g = 3.0f, carbsPer100g = 1.2f, fatPer100g = 1.6f, category = "豆制品"),
        FoodEntity(name = "西兰花", caloriesPer100g = 36f, proteinPer100g = 4.1f, carbsPer100g = 4.3f, fatPer100g = 0.6f, category = "蔬菜"),
        FoodEntity(name = "菠菜", caloriesPer100g = 28f, proteinPer100g = 2.6f, carbsPer100g = 4.5f, fatPer100g = 0.3f, category = "蔬菜"),
        FoodEntity(name = "生菜", caloriesPer100g = 15f, proteinPer100g = 1.3f, carbsPer100g = 2.0f, fatPer100g = 0.3f, category = "蔬菜"),
        FoodEntity(name = "番茄", caloriesPer100g = 20f, proteinPer100g = 0.9f, carbsPer100g = 3.5f, fatPer100g = 0.2f, category = "蔬菜"),
        FoodEntity(name = "黄瓜", caloriesPer100g = 16f, proteinPer100g = 0.8f, carbsPer100g = 2.9f, fatPer100g = 0.2f, category = "蔬菜"),
        FoodEntity(name = "胡萝卜", caloriesPer100g = 32f, proteinPer100g = 1.0f, carbsPer100g = 8.0f, fatPer100g = 0.2f, category = "蔬菜"),
        FoodEntity(name = "土豆", caloriesPer100g = 81f, proteinPer100g = 2.6f, carbsPer100g = 17.8f, fatPer100g = 0.2f, category = "主食"),
        FoodEntity(name = "红薯", caloriesPer100g = 86f, proteinPer100g = 1.6f, carbsPer100g = 20.1f, fatPer100g = 0.1f, category = "主食"),
        FoodEntity(name = "玉米", caloriesPer100g = 112f, proteinPer100g = 4.0f, carbsPer100g = 22.8f, fatPer100g = 1.2f, category = "主食"),
        FoodEntity(name = "苹果", caloriesPer100g = 53f, proteinPer100g = 0.2f, carbsPer100g = 13.7f, fatPer100g = 0.2f, category = "水果"),
        FoodEntity(name = "香蕉", caloriesPer100g = 93f, proteinPer100g = 1.4f, carbsPer100g = 22.0f, fatPer100g = 0.2f, category = "水果"),
        FoodEntity(name = "橙子", caloriesPer100g = 48f, proteinPer100g = 0.8f, carbsPer100g = 11.1f, fatPer100g = 0.2f, category = "水果"),
        FoodEntity(name = "蓝莓", caloriesPer100g = 57f, proteinPer100g = 0.7f, carbsPer100g = 14.5f, fatPer100g = 0.3f, category = "水果"),
        FoodEntity(name = "葡萄", caloriesPer100g = 45f, proteinPer100g = 0.4f, carbsPer100g = 10.3f, fatPer100g = 0.3f, category = "水果"),
        FoodEntity(name = "西瓜", caloriesPer100g = 31f, proteinPer100g = 0.5f, carbsPer100g = 6.8f, fatPer100g = 0.3f, category = "水果"),
        FoodEntity(name = "花生", caloriesPer100g = 567f, proteinPer100g = 25.8f, carbsPer100g = 16.1f, fatPer100g = 49.2f, category = "坚果"),
        FoodEntity(name = "核桃", caloriesPer100g = 646f, proteinPer100g = 14.9f, carbsPer100g = 13.7f, fatPer100g = 58.8f, category = "坚果"),
        FoodEntity(name = "杏仁", caloriesPer100g = 579f, proteinPer100g = 21.3f, carbsPer100g = 21.7f, fatPer100g = 49.9f, category = "坚果"),
        FoodEntity(name = "橄榄油", caloriesPer100g = 884f, proteinPer100g = 0f, carbsPer100g = 0f, fatPer100g = 100f, category = "油脂"),
        FoodEntity(name = "花生油", caloriesPer100g = 899f, proteinPer100g = 0f, carbsPer100g = 0f, fatPer100g = 99.9f, category = "油脂"),
        FoodEntity(name = "酱油", caloriesPer100g = 63f, proteinPer100g = 5.6f, carbsPer100g = 10.1f, fatPer100g = 0.1f, category = "调料"),
        FoodEntity(name = "食盐", caloriesPer100g = 0f, proteinPer100g = 0f, carbsPer100g = 0f, fatPer100g = 0f, category = "调料"),
        FoodEntity(name = "白砂糖", caloriesPer100g = 400f, proteinPer100g = 0f, carbsPer100g = 99.9f, fatPer100g = 0f, category = "调料"),
        FoodEntity(name = "黑巧克力", caloriesPer100g = 546f, proteinPer100g = 4.9f, carbsPer100g = 53.4f, fatPer100g = 34.5f, category = "零食"),
        FoodEntity(name = "乳清蛋白粉", caloriesPer100g = 400f, proteinPer100g = 80f, carbsPer100g = 8f, fatPer100g = 5f, category = "补剂"),
        FoodEntity(name = "美式咖啡", caloriesPer100g = 2f, proteinPer100g = 0.1f, carbsPer100g = 0f, fatPer100g = 0f, category = "饮品")
    )
}
