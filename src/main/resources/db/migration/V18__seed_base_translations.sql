-- Seed base translations for English (en)
INSERT INTO translations (key, language_code, value, category) VALUES
-- Authentication
('auth.login', 'en', 'Login', 'auth'),
('auth.signup', 'en', 'Sign Up', 'auth'),
('auth.email', 'en', 'Email', 'auth'),
('auth.password', 'en', 'Password', 'auth'),
('auth.firstName', 'en', 'First Name', 'auth'),
('auth.lastName', 'en', 'Last Name', 'auth'),
('auth.confirmPassword', 'en', 'Confirm Password', 'auth'),

-- Tabs
('tabs.dashboard', 'en', 'Dashboard', 'tabs'),
('tabs.inventory', 'en', 'Inventory', 'tabs'),
('tabs.employees', 'en', 'Employees', 'tabs'),
('tabs.receipts', 'en', 'Receipts', 'tabs'),
('tabs.profile', 'en', 'Profile', 'tabs'),

-- Inventory
('inventory.title', 'en', 'Inventory Management', 'inventory'),
('inventory.addProduct', 'en', 'Add Product', 'inventory'),
('inventory.editProduct', 'en', 'Edit Product', 'inventory'),
('inventory.deleteProduct', 'en', 'Delete Product', 'inventory'),

-- Employees
('employees.title', 'en', 'Team Management', 'employees'),
('employees.addEmployee', 'en', 'Add Employee', 'employees')
ON CONFLICT (key, language_code) DO NOTHING;

-- Seed base translations for Spanish (es)
INSERT INTO translations (key, language_code, value, category) VALUES
-- Authentication
('auth.login', 'es', 'Iniciar sesión', 'auth'),
('auth.signup', 'es', 'Registrarse', 'auth'),
('auth.email', 'es', 'Correo electrónico', 'auth'),
('auth.password', 'es', 'Contraseña', 'auth'),
('auth.firstName', 'es', 'Nombre', 'auth'),
('auth.lastName', 'es', 'Apellido', 'auth'),
('auth.confirmPassword', 'es', 'Confirmar contraseña', 'auth'),

-- Tabs
('tabs.dashboard', 'es', 'Panel de control', 'tabs'),
('tabs.inventory', 'es', 'Inventario', 'tabs'),
('tabs.employees', 'es', 'Empleados', 'tabs'),
('tabs.receipts', 'es', 'Recibos', 'tabs'),
('tabs.profile', 'es', 'Perfil', 'tabs'),

-- Inventory
('inventory.title', 'es', 'Gestión de inventario', 'inventory'),
('inventory.addProduct', 'es', 'Agregar producto', 'inventory'),
('inventory.editProduct', 'es', 'Editar producto', 'inventory'),
('inventory.deleteProduct', 'es', 'Eliminar producto', 'inventory'),

-- Employees
('employees.title', 'es', 'Gestión de equipo', 'employees'),
('employees.addEmployee', 'es', 'Agregar empleado', 'employees')
ON CONFLICT (key, language_code) DO NOTHING;

-- Seed base translations for Chinese (zh)
INSERT INTO translations (key, language_code, value, category) VALUES
-- Authentication
('auth.login', 'zh', '登录', 'auth'),
('auth.signup', 'zh', '注册', 'auth'),
('auth.email', 'zh', '电子邮件', 'auth'),
('auth.password', 'zh', '密码', 'auth'),
('auth.firstName', 'zh', '名字', 'auth'),
('auth.lastName', 'zh', '姓氏', 'auth'),
('auth.confirmPassword', 'zh', '确认密码', 'auth'),

-- Tabs
('tabs.dashboard', 'zh', '仪表板', 'tabs'),
('tabs.inventory', 'zh', '库存', 'tabs'),
('tabs.employees', 'zh', '员工', 'tabs'),
('tabs.receipts', 'zh', '收据', 'tabs'),
('tabs.profile', 'zh', '个人资料', 'tabs'),

-- Inventory
('inventory.title', 'zh', '库存管理', 'inventory'),
('inventory.addProduct', 'zh', '添加产品', 'inventory'),
('inventory.editProduct', 'zh', '编辑产品', 'inventory'),
('inventory.deleteProduct', 'zh', '删除产品', 'inventory'),

-- Employees
('employees.title', 'zh', '团队管理', 'employees'),
('employees.addEmployee', 'zh', '添加员工', 'employees')
ON CONFLICT (key, language_code) DO NOTHING;
