/**
 * Predefined Items API Client
 * Handles all API calls to the backend for predefined items management
 */

// Configuration
const API_BASE_URL = window.location.origin + '/api/predefined-items';

// Get JWT token from localStorage
function getAuthToken() {
    return localStorage.getItem('authToken') || sessionStorage.getItem('authToken');
}

// Get company ID from localStorage or JWT token
function getCompanyId() {
    // First try to get from localStorage
    let companyId = localStorage.getItem('companyId');
    
    if (!companyId) {
        // Try to extract from JWT token
        const token = getAuthToken();
        if (token) {
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                companyId = payload.tenant_id || payload.companyId || payload.company_id;
            } catch (e) {
                console.error('Failed to parse JWT token:', e);
            }
        }
    }
    
    // For development/testing, you can set a default company ID
    if (!companyId) {
        console.warn('No company ID found. Please set companyId in localStorage or ensure JWT token contains tenant_id');
    }
    
    return companyId;
}

// Helper function to make authenticated API calls
async function apiCall(url, options = {}) {
    const token = getAuthToken();
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };
    
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    
    const config = {
        ...options,
        headers
    };
    
    try {
        const response = await fetch(url, config);
        
        // Handle non-JSON responses (like CSV downloads)
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('text/csv')) {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return { blob: await response.blob(), headers: response.headers };
        }
        
        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.message || `HTTP error! status: ${response.status}`);
        }
        
        return data;
    } catch (error) {
        console.error('API call failed:', error);
        throw error;
    }
}

// API Functions

/**
 * Fetch items with pagination, search, and filters
 */
async function fetchItems({ page = 0, size = 20, search = '', category = '' } = {}) {
    const companyId = getCompanyId();
    if (!companyId) {
        throw new Error('Company ID not found. Please login or set company ID.');
    }
    
    const params = new URLSearchParams({
        companyId,
        page: page.toString(),
        size: size.toString()
    });
    
    if (search) params.append('search', search);
    if (category) params.append('category', category);
    
    const url = `${API_BASE_URL}?${params}`;
    return await apiCall(url, { method: 'GET' });
}

/**
 * Create a new item
 */
async function createItem(itemData) {
    const companyId = getCompanyId();
    if (!companyId) {
        throw new Error('Company ID not found. Please login or set company ID.');
    }
    
    const url = `${API_BASE_URL}?companyId=${companyId}`;
    return await apiCall(url, {
        method: 'POST',
        body: JSON.stringify(itemData)
    });
}

/**
 * Update an existing item
 */
async function updateItem(itemId, itemData) {
    const companyId = getCompanyId();
    if (!companyId) {
        throw new Error('Company ID not found. Please login or set company ID.');
    }
    
    const url = `${API_BASE_URL}/${itemId}?companyId=${companyId}`;
    return await apiCall(url, {
        method: 'PUT',
        body: JSON.stringify(itemData)
    });
}

/**
 * Delete an item (soft delete)
 */
async function deleteItem(itemId) {
    const companyId = getCompanyId();
    if (!companyId) {
        throw new Error('Company ID not found. Please login or set company ID.');
    }
    
    const url = `${API_BASE_URL}/${itemId}?companyId=${companyId}`;
    return await apiCall(url, { method: 'DELETE' });
}

/**
 * Bulk create items
 */
async function bulkCreateItems(items) {
    const companyId = getCompanyId();
    if (!companyId) {
        throw new Error('Company ID not found. Please login or set company ID.');
    }
    
    const url = `${API_BASE_URL}/bulk-create?companyId=${companyId}`;
    return await apiCall(url, {
        method: 'POST',
        body: JSON.stringify(items)
    });
}

/**
 * Import items from CSV
 */
async function importCSV(file) {
    const companyId = getCompanyId();
    if (!companyId) {
        throw new Error('Company ID not found. Please login or set company ID.');
    }
    
    const token = getAuthToken();
    const formData = new FormData();
    formData.append('file', file);
    
    const headers = {};
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    
    const url = `${API_BASE_URL}/import-csv?companyId=${companyId}`;
    
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers,
            body: formData
        });
        
        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.message || `HTTP error! status: ${response.status}`);
        }
        
        return data;
    } catch (error) {
        console.error('CSV import failed:', error);
        throw error;
    }
}

/**
 * Export items to CSV
 */
async function exportCSV() {
    const companyId = getCompanyId();
    if (!companyId) {
        throw new Error('Company ID not found. Please login or set company ID.');
    }
    
    const url = `${API_BASE_URL}/export-csv?companyId=${companyId}`;
    return await apiCall(url, { method: 'GET' });
}

/**
 * Parse CSV file client-side
 */
function parseCSV(csvText) {
    const lines = csvText.split('\n').filter(line => line.trim());
    if (lines.length === 0) {
        return { headers: [], rows: [] };
    }
    
    // Parse headers
    const headers = lines[0].split(',').map(h => h.trim().replace(/^"|"$/g, ''));
    
    // Parse rows
    const rows = [];
    for (let i = 1; i < lines.length; i++) {
        const values = lines[i].split(',').map(v => v.trim().replace(/^"|"$/g, ''));
        if (values.length === headers.length) {
            const row = {};
            headers.forEach((header, index) => {
                row[header] = values[index];
            });
            rows.push(row);
        }
    }
    
    return { headers, rows };
}

/**
 * Validate CSV data
 */
function validateCSV(headers, rows) {
    const warnings = [];
    const requiredHeaders = ['name', 'unitType'];
    
    // Check for required headers
    requiredHeaders.forEach(required => {
        const normalizedHeaders = headers.map(h => h.toLowerCase());
        if (!normalizedHeaders.includes(required.toLowerCase())) {
            warnings.push(`Missing required header: ${required}`);
        }
    });
    
    // Check for empty required fields
    if (warnings.length === 0) {
        rows.forEach((row, index) => {
            if (!row.name || !row.name.trim()) {
                warnings.push(`Row ${index + 2}: Missing required field 'name'`);
            }
            if (!row.unitType && !row.unittype) {
                warnings.push(`Row ${index + 2}: Missing required field 'unitType'`);
            }
        });
    }
    
    return warnings;
}
