package com.edoardo.expensetracker.data.local.dao

import androidx.room.*
import com.edoardo.expensetracker.data.local.entity.CategoryEntity
import com.edoardo.expensetracker.data.local.entity.SubcategoryEntity
import com.edoardo.expensetracker.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

data class CategoryWithSubcategories(
    @Embedded val category: CategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "categoryId"
    )
    val subcategories: List<SubcategoryEntity>
)

@Dao
interface CategoryDao {

    @Transaction
    @Query("SELECT * FROM categories WHERE type = :type AND isArchived = 0 ORDER BY id ASC")
    fun observeCategoriesWithType(type: TransactionType): Flow<List<CategoryWithSubcategories>>

    @Transaction
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun observeAllCategories(): Flow<List<CategoryWithSubcategories>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name = :name AND type = :type LIMIT 1")
    suspend fun getCategoryByNameAndType(name: String, type: TransactionType): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcategory(subcategory: SubcategoryEntity): Long

    @Update
    suspend fun updateSubcategory(subcategory: SubcategoryEntity)

    @Delete
    suspend fun deleteSubcategory(subcategory: SubcategoryEntity)

    @Query("DELETE FROM subcategories WHERE categoryId = :categoryId")
    suspend fun deleteSubcategoriesByCategoryId(categoryId: Long)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoriesCount(): Int
}
