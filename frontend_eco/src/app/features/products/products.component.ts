import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule, Search, ShoppingBag } from 'lucide-angular';
import { ProductService } from '../../core/services/product.service';
import { CartService } from '../../core/services/cart.service';
import { AuthService } from '../../core/services/auth.service';
import { Category, Product } from '../../core/models';

import { PaginationComponent } from '../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [CurrencyPipe, FormsModule, RouterLink, LucideAngularModule, PaginationComponent],
  templateUrl: './products.component.html',
  styleUrl: './products.component.scss'
})
export class ProductsComponent implements OnInit {
  readonly SearchIcon = Search;
  readonly ShoppingBagIcon = ShoppingBag;

  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private authService = inject(AuthService);
  private router = inject(Router);

  searchQuery = signal('');
  selectedCategoryId = signal<number | null>(null);

  allProducts = signal<Product[]>([]);
  categories = signal<Category[]>([]);
  isLoading = signal(false);
  errorMessage = signal('');

  addingProductId = signal<string | null>(null);

  currentPage = signal(1);
  pageSize = 8;

  ngOnInit(): void {
    this.isLoading.set(true);
    this.productService.getProducts().subscribe({
      next: (list) => {
        this.allProducts.set(list || []);
        this.isLoading.set(false);
      },
      error: () => {
        this.allProducts.set([]);
        this.isLoading.set(false);
        this.errorMessage.set('Không tải được danh sách sản phẩm.');
      }
    });

    this.productService.getCategories().subscribe({
      next: (cats) => this.categories.set(cats || []),
      error: () => this.categories.set([])
    });
  }

  filteredProducts = computed(() => {
    let list = this.allProducts();
    const catId = this.selectedCategoryId();
    const query = this.searchQuery().toLowerCase().trim();

    if (catId !== null) {
      list = list.filter(p => p.category?.id === catId);
    }
    if (query) {
      list = list.filter(p => p.name.toLowerCase().includes(query));
    }
    return list;
  });

  paginatedProducts = computed(() => {
    const list = this.filteredProducts();
    const start = (this.currentPage() - 1) * this.pageSize;
    return list.slice(start, start + this.pageSize);
  });

  onSearchChange(value: string): void {
    this.searchQuery.set(value);
    this.currentPage.set(1);
  }

  selectCategory(catId: number | null): void {
    this.selectedCategoryId.set(catId);
    this.currentPage.set(1);
  }

  addToCart(product: Product, event?: Event): void {
    if (event) event.stopPropagation();

    this.addingProductId.set(product.id);

    this.cartService.addItem({
      productId: product.id, quantity: 1,
      productName: product.name, price: product.price, imageUrl: product.imageUrl,
    }).subscribe({
      next: () => {
        this.addingProductId.set(null);
      },
      error: () => {
        this.addingProductId.set(null);
      }
    });
  }
}

