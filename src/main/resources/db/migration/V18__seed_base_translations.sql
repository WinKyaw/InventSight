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

-- Seed base translations for Myanmar (Burmese) (my)
INSERT INTO translations (key, language_code, value, category) VALUES
-- Authentication
('auth.login', 'my', 'အကောင့်ဝင်ရန်', 'auth'),
('auth.signup', 'my', 'အကောင့်ဖွင့်ရန်', 'auth'),
('auth.email', 'my', 'အီးမေးလ်', 'auth'),
('auth.password', 'my', 'လျှို့ဝှက်နံပါတ်', 'auth'),
('auth.confirmPassword', 'my', 'လျှို့ဝှက်နံပါတ်အတည်ပြုရန်', 'auth'),
('auth.firstName', 'my', 'အမည်', 'auth'),
('auth.lastName', 'my', 'မျိုးနွယ်အမည်', 'auth'),
('auth.forgotPassword', 'my', 'လျှို့ဝှက်နံပါတ်မေ့နေပါသလား?', 'auth'),
('auth.createAccount', 'my', 'အကောင့်ဖွင့်မည်', 'auth'),
('auth.signInButton', 'my', 'ဝင်ရောက်မည်', 'auth'),
('auth.acceptTerms', 'my', 'စည်းကမ်းချက်များကို လက်ခံသည်', 'auth'),

-- Tabs/Navigation
('tabs.dashboard', 'my', 'ဒက်ရှ်ဘုတ်', 'tabs'),
('tabs.inventory', 'my', 'ကုန်သိုလှောင်', 'tabs'),
('tabs.receipts', 'my', 'ငွေလက်ခံဖြတ်ပိုင်း', 'tabs'),
('tabs.employees', 'my', 'ဝန်ထမ်းများ', 'tabs'),
('tabs.profile', 'my', 'ကိုယ်ရေးအချက်အလက်', 'tabs'),
('tabs.calendar', 'my', 'ပြက္ခဒိန်', 'tabs'),
('tabs.reports', 'my', 'အစီရင်ခံစာများ', 'tabs'),
('tabs.warehouse', 'my', 'သိုလှောင်ရုံ', 'tabs'),
('tabs.settings', 'my', 'ဆက်တင်များ', 'tabs'),

-- Dashboard
('dashboard.title', 'my', 'ဒက်ရှ်ဘုတ်', 'dashboard'),
('dashboard.subtitle', 'my', 'သင့်လုပ်ငန်းအခြေအနေ', 'dashboard'),
('dashboard.totalSales', 'my', 'စုစုပေါင်းရောင်းချမှု', 'dashboard'),
('dashboard.totalProducts', 'my', 'စုစုပေါင်းကုန်ပစ္စည်း', 'dashboard'),
('dashboard.lowStock', 'my', 'ကုန်ပစ္စည်းနည်းနေသော', 'dashboard'),
('dashboard.activeEmployees', 'my', 'အလုပ်လုပ်နေသောဝန်ထမ်း', 'dashboard'),

-- Inventory
('inventory.title', 'my', 'ကုန်သိုလှောင်စီမံခန့်ခွဲမှု', 'inventory'),
('inventory.addProduct', 'my', 'ကုန်ပစ္စည်းထည့်ရန်', 'inventory'),
('inventory.editProduct', 'my', 'ကုန်ပစ္စည်းပြင်ဆင်ရန်', 'inventory'),
('inventory.deleteProduct', 'my', 'ကုန်ပစ္စည်းဖျက်ရန်', 'inventory'),
('inventory.productName', 'my', 'ကုန်ပစ္စည်းအမည်', 'inventory'),
('inventory.productSKU', 'my', 'SKU ကုဒ်', 'inventory'),
('inventory.category', 'my', 'အမျိုးအစား', 'inventory'),
('inventory.price', 'my', 'စျေးနှုန်း', 'inventory'),
('inventory.quantity', 'my', 'အရေအတွက်', 'inventory'),
('inventory.searchPlaceholder', 'my', 'ကုန်ပစ္စည်းများရှာရန်...', 'inventory'),

-- Employees
('employees.title', 'my', 'အဖွဲ့စီမံခန့်ခွဲမှု', 'employees'),
('employees.addEmployee', 'my', 'ဝန်ထမ်းထည့်ရန်', 'employees'),
('employees.editEmployee', 'my', 'ဝန်ထမ်းပြင်ဆင်ရန်', 'employees'),
('employees.deleteEmployee', 'my', 'ဝန်ထမ်းဖျက်ရန်', 'employees'),
('employees.firstName', 'my', 'အမည်', 'employees'),
('employees.lastName', 'my', 'မျိုးနွယ်အမည်', 'employees'),
('employees.email', 'my', 'အီးမေးလ်', 'employees'),
('employees.title', 'my', 'ရာထူး', 'employees'),
('employees.department', 'my', 'ဌာန', 'employees'),
('employees.hourlyRate', 'my', 'နာရီအလိုက်လုပ်ခ', 'employees'),
('employees.searchPlaceholder', 'my', 'ဝန်ထမ်းများရှာရန်...', 'employees'),

-- Profile
('profile.title', 'my', 'ကိုယ်ရေးအချက်အလက်', 'profile'),
('profile.preferences', 'my', 'နှစ်သက်မှုများ', 'profile'),
('profile.language', 'my', 'ဘာသာစကား', 'profile'),
('profile.theme', 'my', 'အပြင်အဆင်', 'profile'),
('profile.logout', 'my', 'ထွက်ရန်', 'profile'),
('profile.selectLanguage', 'my', 'ဘာသာစကားရွေးချယ်ပါ', 'profile'),

-- Common
('common.save', 'my', 'သိမ်းဆည်းမည်', 'common'),
('common.cancel', 'my', 'မလုပ်တော့ပါ', 'common'),
('common.delete', 'my', 'ဖျက်မည်', 'common'),
('common.edit', 'my', 'ပြင်ဆင်မည်', 'common'),
('common.add', 'my', 'ထည့်မည်', 'common'),
('common.search', 'my', 'ရှာမည်', 'common'),
('common.filter', 'my', 'စစ်ထုတ်မည်', 'common'),
('common.refresh', 'my', 'ပြန်စမည်', 'common'),
('common.loading', 'my', 'တင်နေသည်...', 'common'),
('common.error', 'my', 'အမှား', 'common'),
('common.success', 'my', 'အောင်မြင်သည်', 'common'),
('common.confirm', 'my', 'အတည်ပြုမည်', 'common'),
('common.yes', 'my', 'ဟုတ်ကဲ့', 'common'),
('common.no', 'my', 'မဟုတ်ပါ', 'common'),
('common.ok', 'my', 'OK', 'common'),
('common.back', 'my', 'နောက်သို့', 'common'),
('common.next', 'my', 'ရှေ့သို့', 'common'),

-- Errors
('errors.networkError', 'my', 'အင်တာနက်ချိတ်ဆက်မှုစစ်ဆေးပါ', 'errors'),
('errors.serverError', 'my', 'ဆာဗာအမှား။ နောက်မှထပ်ကြိုးစားပါ', 'errors'),
('errors.unauthorized', 'my', 'ခွင့်ပြုချက်မရှိပါ။ ပြန်ဝင်ပါ', 'errors'),
('errors.genericError', 'my', 'တစ်ခုခုမှားယွင်းနေပါသည်', 'errors')
ON CONFLICT (key, language_code) DO NOTHING;
