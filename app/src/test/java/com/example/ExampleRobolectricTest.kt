package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.Product
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("POS & Inventory", appName)
    }

    @Test
    fun `database pre-populates products and repository performs checkout`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AppDatabase.getInstance(context)
        val repository = PosRepository(database)

        var products = repository.allProducts.first()
        if (products.isEmpty()) {
            database.seedInitialData()
            products = repository.allProducts.first { it.isNotEmpty() }
        }
        assertTrue("Products should be seeded", products.isNotEmpty())

        val testProduct = products.first()
        assertNotNull(testProduct)

        val initialStock = testProduct.stockQuantity
        val cart = listOf(PosRepository.CartItem(testProduct, 1))

        val result = repository.processSale(
            cartItems = cart,
            paymentMethod = "CASH",
            discountPercent = 0.0,
            taxPercent = 8.0,
            notes = "Test sale"
        )

        assertNotNull(result.saleNumber)
        assertEquals(testProduct.sellingPrice * 1.08, result.totalAmount, 0.01)

        val updatedProduct = repository.getProductById(testProduct.id)
        assertNotNull(updatedProduct)
        assertEquals(initialStock - 1, updatedProduct!!.stockQuantity)
    }

    @Test
    fun `sale creates offline sync queue item in Room`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AppDatabase.getInstance(context)
        val repository = PosRepository(database)

        var products = repository.allProducts.first()
        if (products.isEmpty()) {
            database.seedInitialData()
            products = repository.allProducts.first { it.isNotEmpty() }
        }
        val testProduct = products.first()
        val cart = listOf(PosRepository.CartItem(testProduct, 2))

        val result = repository.processSale(
            cartItems = cart,
            paymentMethod = "CARD",
            discountPercent = 0.0,
            taxPercent = 5.0,
            notes = "Offline sync test"
        )

        val pendingSyncItems = repository.getPendingSyncItems()
        assertTrue("Queue should contain pending items", pendingSyncItems.isNotEmpty())

        val queueItem = pendingSyncItems.find { it.saleId == result.saleId }
        assertNotNull("Sync queue item should exist for sale", queueItem)
        assertEquals(result.saleNumber, queueItem!!.saleNumber)
        assertEquals(com.example.data.model.SyncStatus.PENDING, queueItem.syncStatus)

        // Test marking as synced
        repository.markSyncItemSynced(queueItem.id)
        val updatedPending = repository.getPendingSyncItems()
        assertTrue(updatedPending.none { it.id == queueItem.id })
    }
}
