package org.example;

import org.example.warehouse.ProductRecord;
import org.example.warehouse.Warehouse;
import org.junit.jupiter.api.*;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@DisplayName("A warehouse")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WarehouseTest {

    Warehouse warehouse;

    @Test
    @DisplayName("should have no public constructors")
    @Order(1)
    @Tag("basic")
    void shouldHaveNoPublicConstructors() {
        Class<Warehouse> clazz = Warehouse.class;
        Constructor<?>[] constructors = clazz.getConstructors();
        assertEquals(0, constructors.length, "The class should not have any public constructors");
    }

    @Test
    @DisplayName("is created by calling createWarehouse")
    @Order(2)
    @Tag("basic")
    void isCreatedWithFactory() {
        Warehouse warehouse = Warehouse.getInstance();
        assertThat(warehouse).isNotNull();
    }

    @Test
    @DisplayName("can be created with a name")
    @Order(3)
    @Tag("basic")
    void canBeCreatedUsingAName() {
        Warehouse warehouse = Warehouse.getInstance("MyStore");
        assertThat(warehouse).isNotNull().extracting("name").isEqualTo("MyStore");
    }

    @Test
    @DisplayName("should be the same instance when using the same name")
    @Order(4)
    @Tag("basic")
    void shouldBeSameInstanceForSameName() {
        Warehouse warehouse1 = Warehouse.getInstance("Just a name");
        Warehouse warehouse2 = Warehouse.getInstance("Just a name");

        assertThat(warehouse1).isNotNull();
        assertThat(warehouse2).isNotNull();
        assertThat(warehouse1).isSameAs(warehouse2);
    }

    @Nested
    @DisplayName("when new")
    class WhenNew {

        @BeforeEach
        void createWarehouse() {
            warehouse = Warehouse.getInstance("New warehouse");
        }

        @Test
        @DisplayName("is empty")
        void isEmpty() {
            assertThat(warehouse.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("returns empty list of products")
        void returnsEmptyListOfProducts() {
            assertThat(warehouse.getProducts()).isEmpty();
        }

    }

    @Nested
    @DisplayName("after adding one product")
    class AfterAddingProduct {

        ProductRecord addedProduct;
        String UUID_name = "5fc03087-d265-11e7-b8c6-83e29cd24f4c";

        @BeforeEach
        void addingAProduct() {
            warehouse = Warehouse.getInstance("New warehouse");
            addedProduct = warehouse.addProduct(UUID.randomUUID(), "Milk", Category.of("Dairy"), BigDecimal.valueOf(999, 2));
        }

        @Test
        @DisplayName("it is no longer empty")
        void itIsNoLongerEmpty() {
            assertThat(warehouse.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("returns list with that product")
        void getAllShouldReturnListWithOneProduct() {
            assertThat(warehouse.getProducts()).containsExactly(addedProduct);
        }

        @Test
        @DisplayName("valid id returns product")
        void getProductByIdShouldReturnProductWithThatId() {
            assertThat(warehouse.getProductById(addedProduct.uuid())).contains(addedProduct);
        }

        @Test
        @DisplayName("invalid id returns empty")
        void getSingleProductWithInvalidIdShouldBeEmpty() {
            assertThat(warehouse.getProductById(UUID.fromString(UUID_name))).isEmpty();
        }

        @Test
        @DisplayName("throws IllegalArgumentException when using existing id")
        void shouldThrowExceptionIfTryingToAddProductWithSameId() {
            assertThatThrownBy(() ->
                    warehouse.addProduct(UUID.randomUUID(), "Milk", Category.of("Dairy"), BigDecimal.valueOf(999, 2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Product with that id already exists, use updateProduct for updates.");
        }
    }

    @Nested
    @DisplayName("after adding multiple products")
    class AfterAddingMultipleProducts {
        List<ProductRecord> addedProducts = new ArrayList<>();
        String UUID_name = "5fc03087-d265-11e7-b8c6-83e29cd24f4c";

        @BeforeEach
        void addingMultipleProducts() {
            warehouse = Warehouse.getInstance("New warehouse");
            addedProducts.add(warehouse.addProduct(UUID.randomUUID(), "Milk", Category.of("Dairy"), BigDecimal.valueOf(999, 2)));
            addedProducts.add(warehouse.addProduct(UUID.randomUUID(), "Apple", Category.of("Fruit"), BigDecimal.valueOf(290, 2)));
            addedProducts.add(warehouse.addProduct(UUID.randomUUID(), "Bacon", Category.of("Meat"), BigDecimal.valueOf(1567, 2)));
        }

        @Test
        @DisplayName("returns list with all products")
        void returnsListWithAllProducts() {
            assertThat(warehouse.getProducts()).isEqualTo(addedProducts);
        }

        @Test
        @DisplayName("changing a products price should be saved")
        void changingAProductsNameShouldBeSaved() {
            warehouse.updateProductPrice(addedProducts.get(1).uuid(), BigDecimal.valueOf(311, 2));
            assertThat(warehouse.getProductById(addedProducts.get(1).uuid())).isNotEmpty()
                    .get()
                    .hasFieldOrPropertyWithValue("price", BigDecimal.valueOf(311, 2));
        }

        @Test
        @DisplayName("get a map with all products for each category")
        void getAMapWithAllProductsForEachCategory() {
            Map<Category, List<ProductRecord>> productsOfCategories =
                    Map.of(addedProducts.get(0).category(), List.of(addedProducts.get(0)),
                            addedProducts.get(1).category(), List.of(addedProducts.get(1)),
                            addedProducts.get(2).category(), List.of(addedProducts.get(2)));
            assertThat(warehouse.getProductsGroupedByCategories()).isEqualTo(productsOfCategories);
        }

        @Test
        @DisplayName("list returned from getProducts should be unmodifiable")
        void listReturnedFromGetProductsShouldBeImmutable() {
            var products = warehouse.getProducts();
            assertThatThrownBy(() -> products.remove(0))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when trying to change an invalid id")
        void throwsIllegalArgumentExceptionWhenTryingToChangeAnInvalidId() {
            assertThatThrownBy(() ->
                    warehouse.updateProductPrice(UUID.fromString("9e120341-627f-32be-8393-58b5d655b751"), BigDecimal.valueOf(1000, 2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Product with that id doesn't exist.");
        }

        @Test
        @DisplayName("find a product belonging to a category")
        void findProductsBelongingToACategory() {
            assertThat(warehouse.getProductsBy(Category.of("Meat")))
                    .containsOnly(addedProducts.get(2));
        }

        @Test
        @DisplayName("find multiple products from same category")
        void findMultipleProductsFromSameCategory() {
            addedProducts.add(warehouse.addProduct(UUID.randomUUID(), "Steak", Category.of("Meat"), BigDecimal.valueOf(399, 0)));
            assertThat(warehouse.getProductsBy(Category.of("Meat")))
                    .containsOnly(addedProducts.get(2), addedProducts.get(3));
        }


    }


//
//    private Clock fixedClock;
//    private LocalDateTime now;
//
//    @BeforeEach
//    void setUp() {
//        fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
//        now = LocalDateTime.now(fixedClock);
//    }
//
//
//        @Test
//        void Should_AddNewProduct() {
//            Warehouse warehouse = new Warehouse(fixedClock);
//
//            Product product = warehouse.addNewProduct("TestProduct", ProductCategory.BOOKS, 5);
//
//            assertNotNull(product);
//            assertEquals("TestProduct", product.name());
//            assertEquals(ProductCategory.BOOKS, product.category());
//            assertEquals(5, product.rating());
//            assertNotNull(product.id());
//            assertEquals(now, product.createdAt());
//            assertEquals(now, product.updatedAt());
//        }
//
//        @Test
//        void Should_ThrowException_IfNameIsEmpty() {
//            Warehouse warehouse = new Warehouse();
//
//            assertThrows(IllegalArgumentException.class, () ->
//                    warehouse.addNewProduct("", ProductCategory.BOOKS, 5));
//        }
//
//        @Test
//        void Should_ThrowException_IfRatingIsInvalid() {
//            Warehouse warehouse = new Warehouse();
//
//            assertThrows(IllegalArgumentException.class, () ->
//                    warehouse.addNewProduct("Product", ProductCategory.BOOKS, -1));
//            assertThrows(IllegalArgumentException.class, () ->
//                    warehouse.addNewProduct("Product", ProductCategory.BOOKS, 11));
//        }
//
//        @Test
//        void Should_ReturnAllProducts() {
//            Warehouse warehouse = new Warehouse(List.of(
//                    new Product("1", "Product1", ProductCategory.BOOKS, 5, now, now),
//                    new Product("2", "Product2", ProductCategory.BOOKS, 5, now, now)
//            ));
//
//            List<Product> products = warehouse.getAllProducts();
//
//            int expected = 2;
//            int actual = products.size();
//            assertEquals(expected, actual);
//        }
//
//        @Test
//        void Should_ReturnProduct_IfExists() {
//            Product mockProduct = new Product("1", "Product", ProductCategory.BOOKS, 5, now, now);
//            Warehouse warehouse = new Warehouse(List.of(mockProduct));
//
//            Optional<Product> productOptional = warehouse.getProductById(mockProduct.id());
//
//            assertTrue(productOptional.isPresent());
//            assertEquals(mockProduct, productOptional.get());
//        }
//
//        @Test
//        void Should_ReturnEmptyOptional_IfProductNotExists() {
//            Warehouse warehouse = new Warehouse();
//
//            Optional<Product> productOptional = warehouse.getProductById("1");
//
//            assertTrue(productOptional.isEmpty());
//        }
//
//        @Test
//        void Should_UpdateAllProductDetails() {
//            LocalDateTime createdAt = now.minusMinutes(1);
//            Product mockProduct = new Product("1", "Product", ProductCategory.BOOKS, 2, createdAt, createdAt);
//            Warehouse warehouse = new Warehouse(Arrays.asList(mockProduct), fixedClock);
//
//            Product updatedProduct = warehouse.updateProduct(mockProduct.id(), "UpdatedProduct", ProductCategory.VIDEO_GAMES, 5);
//
//            assertEquals("UpdatedProduct", updatedProduct.name());
//            assertEquals(ProductCategory.VIDEO_GAMES, updatedProduct.category());
//            assertEquals(5, updatedProduct.rating());
//            assertTrue(updatedProduct.updatedAt().isAfter(createdAt));
//        }
//
//        @Test
//        void Should_ThrowException_IfInvalidUpdateProductDetails() {
//            Product mockProduct = new Product("1", "Product", ProductCategory.BOOKS, 5, now, now);
//            Warehouse warehouse = new Warehouse(Arrays.asList(mockProduct));
//
//            assertThrows(NoSuchElementException.class, () ->
//                    warehouse.updateProduct("abc", "UpdatedProduct", ProductCategory.MUSIC, 2));
//            assertThrows(IllegalArgumentException.class, () ->
//                    warehouse.updateProduct(mockProduct.id(), "", ProductCategory.MUSIC, 2));
//            assertThrows(IllegalArgumentException.class, () ->
//                    warehouse.updateProduct(mockProduct.id(), "UpdatedProduct", ProductCategory.MUSIC, -1));
//            assertThrows(IllegalArgumentException.class, () ->
//                    warehouse.updateProduct(mockProduct.id(), "UpdatedProduct", ProductCategory.MUSIC, 11));
//        }
//
//        @Test
//        void Should_ReturnProductsInCategorySortedByAlphabeticalOrder() {
//            Warehouse warehouse = new Warehouse(List.of(
//                    new Product("1", "DFG", ProductCategory.BOOKS, 5, now, now),
//                    new Product("2", "HIJ", ProductCategory.MUSIC, 6, now, now),
//                    new Product("3", "ABC", ProductCategory.BOOKS, 8, now, now)
//            ));
//
//            List<Product> booksProducts = warehouse.getProductsByCategory(ProductCategory.BOOKS);
//
//            int expected = 2;
//            int actual = booksProducts.size();
//            assertEquals(expected, actual);
//            assertEquals('A', booksProducts.get(0).name().charAt(0));
//        }
//
//        @Test
//        void Should_ReturnProductsSinceSpecifiedDate() {
//            Warehouse warehouse = new Warehouse(List.of(
//                    new Product("1", "Product1", ProductCategory.BOOKS, 5, now.minusDays(5), now.minusDays(5)),
//                    new Product("2", "Product2", ProductCategory.MUSIC, 6, now.minusDays(3), now.minusDays(3)),
//                    new Product("3", "Product3", ProductCategory.BOOKS, 8, now, now)
//            ));
//
//            LocalDate specifiedDate = now.toLocalDate().minusDays(3);
//            List<Product> products = warehouse.getProductsSince(specifiedDate);
//
//            int expected = 2;
//            int actual = products.size();
//            assertEquals(expected, actual);
//        }
//
//        @Test
//        void Should_ReturnModifiedProducts() {
//            Warehouse warehouse = new Warehouse(List.of(
//                    new Product("1", "Product1", ProductCategory.BOOKS, 8, now, now.plusMinutes(1)),
//                    new Product("2", "Product2", ProductCategory.BOOKS, 8, now, now),
//                    new Product("3", "Product3", ProductCategory.BOOKS, 8, now, now.plusHours(1))
//            ));
//
//            List<Product> products = warehouse.getModifiedProducts();
//
//            int expected = 2;
//            int actual = products.size();
//            assertEquals(expected, actual);
//        }
//
//        @Test
//        void Should_ReturnCategoriesTiedToAtLeast1Product() {
//            Warehouse warehouse = new Warehouse(List.of(
//                    new Product("1", "Product1", ProductCategory.BOOKS, 8, now, now),
//                    new Product("2", "Product2", ProductCategory.VIDEO_GAMES, 4, now, now),
//                    new Product("3", "Product3", ProductCategory.BOOKS, 3, now, now)
//            ));
//
//            Set<ProductCategory> categories = warehouse.getCategoriesWithProducts();
//
//            int expected = 2;
//            int actual = categories.size();
//            assertEquals(expected, actual);
//        }
//
//        @Test
//        void Should_ReturnProductCountInSpecifiedCategory() {
//            Warehouse warehouse = new Warehouse(List.of(
//                    new Product("1", "Product1", ProductCategory.BOOKS, 8, now, now),
//                    new Product("2", "Product2", ProductCategory.VIDEO_GAMES, 4, now, now),
//                    new Product("3", "Product3", ProductCategory.BOOKS, 3, now, now)
//            ));
//
//            long productCount = warehouse.getProductCountInCategory(ProductCategory.BOOKS);
//
//            int expected = 2;
//            assertEquals(expected, productCount);
//        }
//
//        @Test
//        void Should_ReturnProductsWithMaxRatingThisMonthSortedByNewest() {
//            Warehouse warehouse = new Warehouse(List.of(
//                    new Product("1", "Product1", ProductCategory.BOOKS, 10, now.minusMonths(1), now.minusMonths(1)),
//                    new Product("2", "Product2", ProductCategory.BOOKS, 10, now.minusMinutes(2), now.minusMinutes(2)),
//                    new Product("3", "Product3", ProductCategory.BOOKS, 10, now.minusMinutes(1), now.minusMinutes(1)),
//                    new Product("4", "Product4", ProductCategory.BOOKS, 9, now, now)
//            ));
//
//            List<Product> products = warehouse.getTopRatedProductsThisMonth();
//
//            int expected = 2;
//            int actual = products.size();
//            assertEquals(expected, actual);
//            boolean condition = products.get(0).createdAt().isAfter(products.get(1).createdAt());
//            assertTrue(condition);
//        }
//
//        @Test
//        void Should_ReturnProductCountByFirstLetter() {
//            Warehouse warehouse = new Warehouse(List.of(
//                    new Product("1", "Abc", ProductCategory.BOOKS, 10, now, now),
//                    new Product("2", "Bcd", ProductCategory.BOOKS, 10, now, now),
//                    new Product("3", "Abc", ProductCategory.BOOKS, 10, now, now)
//            ));
//
//            Map<Character, Long> productMap = warehouse.getProductCountByFirstLetter();
//
//            int expected = 2;
//            int actual = productMap.size();
//            assertEquals(expected, actual);
//            assertEquals(2L, productMap.get('A'));
//        }
//
//        @Test
//        void Should_ReturnEmptyResult_When_NoProductsFound() {
//            Warehouse warehouse = new Warehouse();
//
//            List<Product> allProducts = warehouse.getAllProducts();
//            List<Product> booksProducts = warehouse.getProductsByCategory(ProductCategory.BOOKS);
//            List<Product> productsSince = warehouse.getProductsSince(now.toLocalDate());
//            List<Product> modifiedProducts = warehouse.getModifiedProducts();
//            Set<ProductCategory> categories = warehouse.getCategoriesWithProducts();
//            List<Product> topRatedProducts = warehouse.getTopRatedProductsThisMonth();
//            Map<Character, Long> productMap = warehouse.getProductCountByFirstLetter();
//
//            assertTrue(allProducts.isEmpty());
//            assertTrue(booksProducts.isEmpty());
//            assertTrue(productsSince.isEmpty());
//            assertTrue(modifiedProducts.isEmpty());
//            assertTrue(categories.isEmpty());
//            assertTrue(topRatedProducts.isEmpty());
//            assertTrue(productMap.isEmpty());
//        }
}
