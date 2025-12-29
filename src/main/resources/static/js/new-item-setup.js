/**
 * New Item Setup - Main UI Controller
 * Handles all user interactions and UI updates
 */

// State
let currentPage = 0;
let totalPages = 0;
let currentSearch = '';
let currentCategory = '';
let editingItemId = null;
let deletingItemId = null;
let csvData = null;

// DOM Elements - will be initialized when DOM is ready
let elements = {};

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    initializeElements();
    initializeEventListeners();
    loadItems();
    loadCategories();
});

/**
 * Initialize DOM element references
 */
function initializeElements() {
    elements = {
        // Buttons
        addItemBtn: document.getElementById('addItemBtn'),
        importCsvBtn: document.getElementById('importCsvBtn'),
        exportCsvBtn: document.getElementById('exportCsvBtn'),
        clearFiltersBtn: document.getElementById('clearFiltersBtn'),
        
        // Search and Filter
        searchInput: document.getElementById('searchInput'),
        categoryFilter: document.getElementById('categoryFilter'),
        
        // States
        loadingState: document.getElementById('loadingState'),
        emptyState: document.getElementById('emptyState'),
        itemsTable: document.getElementById('itemsTable'),
        itemsTableBody: document.getElementById('itemsTableBody'),
        
        // Pagination
        paginationContainer: document.getElementById('paginationContainer'),
        prevPageBtn: document.getElementById('prevPageBtn'),
        nextPageBtn: document.getElementById('nextPageBtn'),
        pageInfo: document.getElementById('pageInfo'),
        
        // Item Modal
        itemModal: document.getElementById('itemModal'),
        modalTitle: document.getElementById('modalTitle'),
        itemForm: document.getElementById('itemForm'),
        closeModalBtn: document.getElementById('closeModalBtn'),
        cancelModalBtn: document.getElementById('cancelModalBtn'),
        saveItemBtn: document.getElementById('saveItemBtn'),
        saveButtonText: document.getElementById('saveButtonText'),
        saveButtonSpinner: document.getElementById('saveButtonSpinner'),
        
        // Form Fields
        itemName: document.getElementById('itemName'),
        itemCategory: document.getElementById('itemCategory'),
        itemUnitType: document.getElementById('itemUnitType'),
        itemDescription: document.getElementById('itemDescription'),
        itemDefaultPrice: document.getElementById('itemDefaultPrice'),
        
        // Error Messages
        nameError: document.getElementById('nameError'),
        unitTypeError: document.getElementById('unitTypeError'),
        
        // CSV Import Modal
        csvImportModal: document.getElementById('csvImportModal'),
        csvFileInput: document.getElementById('csvFileInput'),
        selectCsvBtn: document.getElementById('selectCsvBtn'),
        selectedFileName: document.getElementById('selectedFileName'),
        csvPreview: document.getElementById('csvPreview'),
        csvPreviewTable: document.getElementById('csvPreviewTable'),
        csvWarnings: document.getElementById('csvWarnings'),
        closeCsvModalBtn: document.getElementById('closeCsvModalBtn'),
        cancelCsvBtn: document.getElementById('cancelCsvBtn'),
        confirmImportBtn: document.getElementById('confirmImportBtn'),
        
        // Delete Modal
        deleteModal: document.getElementById('deleteModal'),
        deleteMessage: document.getElementById('deleteMessage'),
        closeDeleteModalBtn: document.getElementById('closeDeleteModalBtn'),
        cancelDeleteBtn: document.getElementById('cancelDeleteBtn'),
        confirmDeleteBtn: document.getElementById('confirmDeleteBtn'),
        
        // Toast
        toast: document.getElementById('toast')
    };
}

/**
 * Initialize event listeners
 */
function initializeEventListeners() {
    // Add Item
    elements.addItemBtn.addEventListener('click', openAddModal);
    
    // Export CSV
    elements.exportCsvBtn.addEventListener('click', handleExportCSV);
    
    // Import CSV
    elements.importCsvBtn.addEventListener('click', () => elements.csvImportModal.style.display = 'block');
    elements.selectCsvBtn.addEventListener('click', () => elements.csvFileInput.click());
    elements.csvFileInput.addEventListener('change', handleCsvFileSelect);
    elements.closeCsvModalBtn.addEventListener('click', closeCsvModal);
    elements.cancelCsvBtn.addEventListener('click', closeCsvModal);
    elements.confirmImportBtn.addEventListener('click', handleCsvImport);
    
    // Search with debouncing
    let searchTimeout;
    elements.searchInput.addEventListener('input', (e) => {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            currentSearch = e.target.value;
            currentPage = 0;
            loadItems();
        }, 500);
    });
    
    // Category Filter
    elements.categoryFilter.addEventListener('change', (e) => {
        currentCategory = e.target.value;
        currentPage = 0;
        loadItems();
    });
    
    // Clear Filters
    elements.clearFiltersBtn.addEventListener('click', clearFilters);
    
    // Pagination
    elements.prevPageBtn.addEventListener('click', () => {
        if (currentPage > 0) {
            currentPage--;
            loadItems();
        }
    });
    
    elements.nextPageBtn.addEventListener('click', () => {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadItems();
        }
    });
    
    // Item Modal
    elements.closeModalBtn.addEventListener('click', closeItemModal);
    elements.cancelModalBtn.addEventListener('click', closeItemModal);
    elements.itemForm.addEventListener('submit', handleItemFormSubmit);
    
    // Delete Modal
    elements.closeDeleteModalBtn.addEventListener('click', closeDeleteModal);
    elements.cancelDeleteBtn.addEventListener('click', closeDeleteModal);
    elements.confirmDeleteBtn.addEventListener('click', handleConfirmDelete);
    
    // Close modals on outside click
    window.addEventListener('click', (e) => {
        if (e.target === elements.itemModal) closeItemModal();
        if (e.target === elements.csvImportModal) closeCsvModal();
        if (e.target === elements.deleteModal) closeDeleteModal();
    });
}

/**
 * Load items from API
 */
async function loadItems() {
    showLoading();
    
    try {
        const response = await fetchItems({
            page: currentPage,
            size: 20,
            search: currentSearch,
            category: currentCategory
        });
        
        if (response.success && response.data) {
            const { items, totalPages: total, currentPage: current } = response.data;
            totalPages = total;
            currentPage = current;
            
            if (items && items.length > 0) {
                displayItems(items);
                updatePagination();
            } else {
                showEmptyState();
            }
        } else {
            showEmptyState();
        }
    } catch (error) {
        console.error('Failed to load items:', error);
        showToast('Failed to load items: ' + error.message, 'error');
        showEmptyState();
    }
}

/**
 * Load unique categories for filter
 */
async function loadCategories() {
    try {
        const response = await fetchItems({ size: 1000 }); // Get all to extract categories
        if (response.success && response.data && response.data.items) {
            const categories = [...new Set(response.data.items
                .map(item => item.category)
                .filter(cat => cat && cat.trim())
            )].sort();
            
            // Populate category filter
            elements.categoryFilter.innerHTML = '<option value="">All Categories</option>';
            categories.forEach(category => {
                const option = document.createElement('option');
                option.value = category;
                option.textContent = category;
                elements.categoryFilter.appendChild(option);
            });
        }
    } catch (error) {
        console.error('Failed to load categories:', error);
    }
}

/**
 * Display items in table
 */
function displayItems(items) {
    elements.loadingState.style.display = 'none';
    elements.emptyState.style.display = 'none';
    elements.itemsTable.style.display = 'block';
    elements.paginationContainer.style.display = 'flex';
    
    elements.itemsTableBody.innerHTML = items.map(item => `
        <tr data-item-id="${escapeHtml(item.id)}">
            <td>${escapeHtml(item.name)}</td>
            <td>${escapeHtml(item.category || '-')}</td>
            <td>${escapeHtml(item.unitType)}</td>
            <td>${escapeHtml(item.sku || 'Auto')}</td>
            <td>${item.defaultPrice ? '$' + item.defaultPrice.toFixed(2) : '-'}</td>
            <td>
                <div class="item-actions">
                    <button class="icon-btn edit" data-action="edit" data-item-id="${escapeHtml(item.id)}" title="Edit">
                        <i class="fas fa-pencil-alt"></i>
                    </button>
                    <button class="icon-btn delete" data-action="delete" data-item-id="${escapeHtml(item.id)}" data-item-name="${escapeHtml(item.name)}" title="Delete">
                        <i class="fas fa-trash-alt"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
    
    // Add event listeners for action buttons
    document.querySelectorAll('.icon-btn[data-action="edit"]').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const itemId = e.currentTarget.getAttribute('data-item-id');
            openEditModal(itemId);
        });
    });
    
    document.querySelectorAll('.icon-btn[data-action="delete"]').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const itemId = e.currentTarget.getAttribute('data-item-id');
            const itemName = e.currentTarget.getAttribute('data-item-name');
            openDeleteModal(itemId, itemName);
        });
    });
}

/**
 * Show loading state
 */
function showLoading() {
    elements.loadingState.style.display = 'block';
    elements.emptyState.style.display = 'none';
    elements.itemsTable.style.display = 'none';
    elements.paginationContainer.style.display = 'none';
}

/**
 * Show empty state
 */
function showEmptyState() {
    elements.loadingState.style.display = 'none';
    elements.emptyState.style.display = 'block';
    elements.itemsTable.style.display = 'none';
    elements.paginationContainer.style.display = 'none';
}

/**
 * Update pagination controls
 */
function updatePagination() {
    elements.pageInfo.textContent = `Page ${currentPage + 1} of ${totalPages || 1}`;
    elements.prevPageBtn.disabled = currentPage === 0;
    elements.nextPageBtn.disabled = currentPage >= totalPages - 1;
}

/**
 * Clear all filters
 */
function clearFilters() {
    elements.searchInput.value = '';
    elements.categoryFilter.value = '';
    currentSearch = '';
    currentCategory = '';
    currentPage = 0;
    loadItems();
}

/**
 * Open Add Item Modal
 */
function openAddModal() {
    editingItemId = null;
    elements.modalTitle.textContent = 'Add Item';
    elements.saveButtonText.textContent = 'Save Item';
    elements.itemForm.reset();
    clearFormErrors();
    elements.itemModal.style.display = 'block';
}

/**
 * Open Edit Item Modal
 */
async function openEditModal(itemId) {
    editingItemId = itemId;
    elements.modalTitle.textContent = 'Edit Item';
    elements.saveButtonText.textContent = 'Update Item';
    clearFormErrors();
    
    // Find item in current data
    try {
        const response = await fetchItems({ size: 1000 });
        if (response.success && response.data && response.data.items) {
            const item = response.data.items.find(i => i.id === itemId);
            if (item) {
                elements.itemName.value = item.name || '';
                elements.itemCategory.value = item.category || '';
                elements.itemUnitType.value = item.unitType || '';
                elements.itemDescription.value = item.description || '';
                elements.itemDefaultPrice.value = item.defaultPrice || '';
                elements.itemModal.style.display = 'block';
            } else {
                showToast('Item not found', 'error');
            }
        }
    } catch (error) {
        showToast('Failed to load item details: ' + error.message, 'error');
    }
}

/**
 * Close Item Modal
 */
function closeItemModal() {
    elements.itemModal.style.display = 'none';
    elements.itemForm.reset();
    editingItemId = null;
    clearFormErrors();
}

/**
 * Handle Item Form Submit
 */
async function handleItemFormSubmit(e) {
    e.preventDefault();
    clearFormErrors();
    
    // Validate
    let hasErrors = false;
    if (!elements.itemName.value.trim()) {
        elements.nameError.textContent = 'Name is required';
        hasErrors = true;
    }
    if (!elements.itemUnitType.value.trim()) {
        elements.unitTypeError.textContent = 'Unit type is required';
        hasErrors = true;
    }
    
    if (hasErrors) return;
    
    // Prepare data
    const itemData = {
        name: elements.itemName.value.trim(),
        category: elements.itemCategory.value.trim() || null,
        unitType: elements.itemUnitType.value.trim(),
        description: elements.itemDescription.value.trim() || null,
        defaultPrice: elements.itemDefaultPrice.value ? parseFloat(elements.itemDefaultPrice.value) : null
    };
    
    // Show loading
    elements.saveItemBtn.disabled = true;
    elements.saveButtonText.style.display = 'none';
    elements.saveButtonSpinner.style.display = 'inline-block';
    
    try {
        if (editingItemId) {
            // Update
            await updateItem(editingItemId, itemData);
            showToast('Item updated successfully', 'success');
        } else {
            // Create
            await createItem(itemData);
            showToast('Item added successfully', 'success');
        }
        
        closeItemModal();
        loadItems();
        loadCategories(); // Refresh categories
    } catch (error) {
        console.error('Failed to save item:', error);
        showToast(error.message || 'Failed to save item', 'error');
    } finally {
        elements.saveItemBtn.disabled = false;
        elements.saveButtonText.style.display = 'inline';
        elements.saveButtonSpinner.style.display = 'none';
    }
}

/**
 * Clear form errors
 */
function clearFormErrors() {
    elements.nameError.textContent = '';
    elements.unitTypeError.textContent = '';
}

/**
 * Open Delete Modal
 */
function openDeleteModal(itemId, itemName) {
    deletingItemId = itemId;
    elements.deleteMessage.textContent = 
        `Are you sure you want to delete "${itemName}"? This will mark it as inactive.`;
    elements.deleteModal.style.display = 'block';
}

/**
 * Close Delete Modal
 */
function closeDeleteModal() {
    elements.deleteModal.style.display = 'none';
    deletingItemId = null;
}

/**
 * Handle Confirm Delete
 */
async function handleConfirmDelete() {
    if (!deletingItemId) return;
    
    // Show loading
    const deleteBtn = elements.confirmDeleteBtn;
    const spinner = deleteBtn.querySelector('.button-spinner');
    const text = deleteBtn.querySelector('span:first-child');
    
    deleteBtn.disabled = true;
    text.style.display = 'none';
    spinner.style.display = 'inline-block';
    
    try {
        await deleteItem(deletingItemId);
        showToast('Item deleted successfully', 'success');
        closeDeleteModal();
        loadItems();
    } catch (error) {
        console.error('Failed to delete item:', error);
        showToast(error.message || 'Failed to delete item', 'error');
    } finally {
        deleteBtn.disabled = false;
        text.style.display = 'inline';
        spinner.style.display = 'none';
    }
}

/**
 * Handle CSV File Select
 */
async function handleCsvFileSelect(e) {
    const file = e.target.files[0];
    if (!file) return;
    
    elements.selectedFileName.textContent = file.name;
    
    try {
        const text = await file.text();
        const { headers, rows } = parseCSV(text);
        
        csvData = { file, headers, rows };
        
        // Show preview
        displayCsvPreview(headers, rows.slice(0, 5));
        
        // Validate
        const warnings = validateCSV(headers, rows);
        if (warnings.length > 0) {
            displayCsvWarnings(warnings);
        } else {
            elements.csvWarnings.innerHTML = '';
        }
        
        elements.csvPreview.style.display = 'block';
    } catch (error) {
        showToast('Failed to parse CSV file: ' + error.message, 'error');
    }
}

/**
 * Display CSV Preview
 */
function displayCsvPreview(headers, rows) {
    let html = '<thead><tr>';
    headers.forEach(header => {
        html += `<th>${escapeHtml(header)}</th>`;
    });
    html += '</tr></thead><tbody>';
    
    rows.forEach(row => {
        html += '<tr>';
        headers.forEach(header => {
            html += `<td>${escapeHtml(row[header] || '')}</td>`;
        });
        html += '</tr>';
    });
    
    html += '</tbody>';
    elements.csvPreviewTable.innerHTML = html;
}

/**
 * Display CSV Warnings
 */
function displayCsvWarnings(warnings) {
    let html = '<strong>Warnings:</strong><ul>';
    warnings.forEach(warning => {
        html += `<li>${escapeHtml(warning)}</li>`;
    });
    html += '</ul>';
    elements.csvWarnings.innerHTML = html;
}

/**
 * Handle CSV Import
 */
async function handleCsvImport() {
    if (!csvData || !csvData.file) {
        showToast('Please select a CSV file first', 'error');
        return;
    }
    
    // Show loading
    const importBtn = elements.confirmImportBtn;
    const spinner = importBtn.querySelector('.button-spinner');
    const text = importBtn.querySelector('span:first-child');
    
    importBtn.disabled = true;
    text.style.display = 'none';
    spinner.style.display = 'inline-block';
    
    try {
        const response = await importCSV(csvData.file);
        
        if (response.success && response.data) {
            const { successful, failed, total } = response.data;
            
            if (failed === 0) {
                showToast(`Successfully imported ${successful} items`, 'success');
            } else {
                showToast(`Imported ${successful} of ${total} items. ${failed} failed.`, 'warning');
            }
            
            closeCsvModal();
            loadItems();
            loadCategories();
        } else {
            showToast('Import failed', 'error');
        }
    } catch (error) {
        console.error('Failed to import CSV:', error);
        showToast(error.message || 'Failed to import CSV', 'error');
    } finally {
        importBtn.disabled = false;
        text.style.display = 'inline';
        spinner.style.display = 'none';
    }
}

/**
 * Close CSV Modal
 */
function closeCsvModal() {
    elements.csvImportModal.style.display = 'none';
    elements.csvFileInput.value = '';
    elements.selectedFileName.textContent = '';
    elements.csvPreview.style.display = 'none';
    csvData = null;
}

/**
 * Handle CSV Export
 */
async function handleExportCSV() {
    try {
        const response = await exportCSV();
        
        if (response.blob) {
            // Create download link
            const url = window.URL.createObjectURL(response.blob);
            const a = document.createElement('a');
            a.href = url;
            
            // Get filename from header or use default
            const contentDisposition = response.headers.get('content-disposition');
            let filename = 'predefined-items-' + new Date().toISOString().split('T')[0] + '.csv';
            if (contentDisposition) {
                const matches = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(contentDisposition);
                if (matches != null && matches[1]) {
                    filename = matches[1].replace(/['"]/g, '');
                }
            }
            
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
            
            showToast('CSV exported successfully', 'success');
        }
    } catch (error) {
        console.error('Failed to export CSV:', error);
        showToast(error.message || 'Failed to export CSV', 'error');
    }
}

/**
 * Show toast notification
 */
function showToast(message, type = 'success') {
    elements.toast.textContent = message;
    elements.toast.className = 'toast ' + type;
    elements.toast.style.display = 'block';
    
    setTimeout(() => {
        elements.toast.style.display = 'none';
    }, 4000);
}

/**
 * Escape HTML to prevent XSS
 */
function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return String(text || '').replace(/[&<>"']/g, m => map[m]);
}

// Note: openEditModal and openDeleteModal are no longer needed globally
// as they are now attached via event listeners instead of onclick handlers
