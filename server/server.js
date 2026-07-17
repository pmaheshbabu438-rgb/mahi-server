const express = require('express');
const cors = require('cors');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { Pool } = require('pg');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'signalconnect_super_encrypter_secret_67890';

// Enable middlewares
app.use(cors());
app.use(express.json());

// Set up PostgreSQL connection Pool with clean fallback
let dbPool = null;
let useLocalMemoryDb = false;

if (process.env.DATABASE_URL || (process.env.DB_HOST && process.env.DB_USER)) {
  const config = process.env.DATABASE_URL
    ? { connectionString: process.env.DATABASE_URL, ssl: { rejectUnauthorized: false } }
    : {
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        port: process.env.DB_PORT || 5432,
      };

  dbPool = new Pool(config);
  dbPool.query('SELECT NOW()', (err, res) => {
    if (err) {
      console.warn('⚠️ WARNING: PostgreSQL failed to connect. Defaulting to high-performance memory dataset database fallback.');
      useLocalMemoryDb = true;
    } else {
      console.log('✅ PostgreSQL connected successfully at:', res.rows[0].now);
    }
  });
} else {
  console.log('💡 DB Configuration is empty. Defaulting to high-performance memory dataset database fallback.');
  useLocalMemoryDb = true;
}

// -------------------------------------------------------------
// Core Mock Memory Database (Fallbacks or standard boots)
// -------------------------------------------------------------
const memoryUsers = [
  {
    id: 'admin_sys_def',
    email: 'admin@signalconnect.com',
    name: 'Admin Commander',
    passwordHash: bcrypt.hashSync('admin123', 10),
    isAdmin: true
  }
];

const memoryRouters = [
  {
    bssid: '00:1A:2B:3C:4D:5E',
    ssid: 'SignalConnect_Alpha-7',
    signalStrengthDbm: -42,
    downloadSpeedMbps: 85.5,
    uploadSpeedMbps: 35.2,
    latencyMs: 12,
    distanceMeters: 3.2,
    securityType: 'WPA3 Secure',
    isRegistered: true,
    capacityMax: 150,
    capacityActive: 18,
    billingRate: 'Free Community Link',
    description: 'Ultra-speed node deployed in Central Transit Hub.'
  },
  {
    bssid: '24:F5:A2:8B:10:9C',
    ssid: 'Downtown_Greenway_Node',
    signalStrengthDbm: -65,
    downloadSpeedMbps: 38.0,
    uploadSpeedMbps: 12.5,
    latencyMs: 28,
    distanceMeters: 14.8,
    securityType: 'WPA2 Enterprise',
    isRegistered: true,
    capacityMax: 80,
    capacityActive: 47,
    billingRate: 'Free Public',
    description: 'Outdoor greenway coverage zone.'
  },
  {
    bssid: 'E2:81:B4:9C:FD:0F',
    ssid: 'RescueNet_Gateway_Base',
    signalStrengthDbm: -81,
    downloadSpeedMbps: 15.2,
    uploadSpeedMbps: 4.0,
    latencyMs: 45,
    distanceMeters: 24.5,
    securityType: 'Open Gateway',
    isRegistered: true,
    capacityMax: 200,
    capacityActive: 104,
    billingRate: 'Free Emergency',
    description: 'Weak signal cellular extender provided during local outages.'
  }
];

// Helper: Token Generator
function generateToken(user) {
  return jwt.sign(
    { id: user.id, email: user.email, name: user.name, isAdmin: user.isAdmin },
    JWT_SECRET,
    { expiresIn: '30d' }
  );
}

// Helper Middleware: JWT verify
function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ success: false, message: 'Access denied. Missing bearer token.' });
  }

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) {
      return res.status(403).json({ success: false, message: 'Invalid or expired bearer token.' });
    }
    req.user = user;
    next();
  });
}

// -------------------------------------------------------------
// REST API Endpoints
// -------------------------------------------------------------

// Default Root status check page
app.get('/', (req, res) => {
  res.json({
    success: true,
    message: 'SignalConnect Secure REST Server is actively running.',
    mode: useLocalMemoryDb ? 'Memory-Sandbox' : 'PostgreSQL-Production'
  });
});

// Endpoint 1: Register User
app.post('/api/auth/register', async (req, res) => {
  const { email, name, password, isAdmin } = req.body;

  if (!email || !name || !password) {
    return res.status(400).json({ success: false, message: 'Email, name, and password are required.' });
  }

  const hash = await bcrypt.hash(password, 10);
  const adminRights = !!isAdmin;

  if (useLocalMemoryDb) {
    const exists = memoryUsers.some(u => u.email.toLowerCase() === email.toLowerCase());
    if (exists) {
      return res.status(400).json({ success: false, message: 'Email address already registered.' });
    }

    const newUser = {
      id: `user_${Date.now()}`,
      email: email.toLowerCase(),
      name,
      passwordHash: hash,
      isAdmin: adminRights
    };
    memoryUsers.push(newUser);

    const token = generateToken(newUser);
    return res.status(201).json({
      success: true,
      message: 'Account registered successfully.',
      data: { id: newUser.id, email: newUser.email, name: newUser.name, token, isAdmin: newUser.isAdmin }
    });
  } else {
    try {
      const existsQuery = await dbPool.query('SELECT id FROM users WHERE email = $1', [email.toLowerCase()]);
      if (existsQuery.rows.length > 0) {
        return res.status(400).json({ success: false, message: 'Email address already registered.' });
      }

      const createQuery = await dbPool.query(
        'INSERT INTO users (email, name, password_hash, is_admin) VALUES ($1, $2, $3, $4) RETURNING id, email, name, is_admin',
        [email.toLowerCase(), name, hash, adminRights]
      );
      const user = createQuery.rows[0];
      const token = generateToken({ id: user.id, email: user.email, name: user.name, isAdmin: user.is_admin });

      return res.status(201).json({
        success: true,
        message: 'Account registered successfully.',
        data: { id: user.id, email: user.email, name: user.name, token, isAdmin: user.is_admin }
      });
    } catch (e) {
      console.error(e);
      return res.status(500).json({ success: false, message: 'Database communication error registering account.' });
    }
  }
});

// Endpoint 2: Login User
app.post('/api/auth/login', async (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ success: false, message: 'Email and password fields must not be empty.' });
  }

  if (useLocalMemoryDb) {
    const user = memoryUsers.find(u => u.email.toLowerCase() === email.toLowerCase());
    if (!user) {
      return res.status(401).json({ success: false, message: 'Invalid email address or password credentials.' });
    }

    const isMatch = await bcrypt.compare(password, user.passwordHash);
    if (!isMatch) {
      return res.status(401).json({ success: false, message: 'Invalid email address or password credentials.' });
    }

    const token = generateToken(user);
    return res.json({
      success: true,
      message: 'Handshake successful.',
      data: { id: user.id, email: user.email, name: user.name, token, isAdmin: user.isAdmin }
    });
  } else {
    try {
      const result = await dbPool.query('SELECT * FROM users WHERE email = $1', [email.toLowerCase()]);
      if (result.rows.length === 0) {
        return res.status(401).json({ success: false, message: 'Invalid email address or password credentials.' });
      }

      const user = result.rows[0];
      const isMatch = await bcrypt.compare(password, user.password_hash);
      if (!isMatch) {
        return res.status(401).json({ success: false, message: 'Invalid email address or password credentials.' });
      }

      const token = generateToken({ id: user.id, email: user.email, name: user.name, isAdmin: user.is_admin });
      return res.json({
        success: true,
        message: 'Handshake successful.',
        data: { id: user.id, email: user.email, name: user.name, token, isAdmin: user.is_admin }
      });
    } catch (e) {
      console.error(e);
      return res.status(500).json({ success: false, message: 'Database transaction error in authentication.' });
    }
  }
});

// Endpoint 3: Sync/Get Registered Routers (Secured)
app.get('/api/routers', authenticateToken, async (req, res) => {
  if (useLocalMemoryDb) {
    return res.json({ success: true, data: memoryRouters });
  } else {
    try {
      const result = await dbPool.query('SELECT * FROM routers ORDER BY created_at DESC');
      const list = result.rows.map(r => ({
        bssid: r.bssid,
        ssid: r.ssid,
        signalStrengthDbm: r.signal_strength_dbm,
        downloadSpeedMbps: r.download_speed_mbps,
        uploadSpeedMbps: r.upload_speed_mbps,
        latencyMs: r.latency_ms,
        distanceMeters: r.distance_meters,
        securityType: r.security_type,
        isRegistered: r.is_registered,
        capacityMax: r.capacity_max,
        capacityActive: r.capacity_active,
        billingRate: r.billing_rate,
        description: r.description
      }));
      return res.json({ success: true, data: list });
    } catch (e) {
      console.error(e);
      return res.status(500).json({ success: false, message: 'Failed to retrieve nodes from Postgres.' });
    }
  }
});

// Endpoint 4: Register/Add Custom Node (Secured + Admin Only)
app.post('/api/routers', authenticateToken, async (req, res) => {
  const { bssid, ssid, downloadSpeedMbps, uploadSpeedMbps, securityType, billingRate, description } = req.body;

  if (!bssid || !ssid || !downloadSpeedMbps || !uploadSpeedMbps) {
    return res.status(400).json({ success: false, message: 'BSSID, SSID, and bandwidth parameters are mandatory.' });
  }

  const isBssidMacMatch = /^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$/.test(bssid);
  if (!isBssidMacMatch) {
    return res.status(400).json({ success: false, message: 'Mandatory MAC format violated. E.g. AA:BB:CC:DD:EE:FF' });
  }

  const newRouter = {
    bssid,
    ssid,
    signalStrengthDbm: -55,
    downloadSpeedMbps: parseFloat(downloadSpeedMbps),
    uploadSpeedMbps: parseFloat(uploadSpeedMbps),
    latencyMs: 18,
    distanceMeters: 6.5,
    securityType: securityType || 'WPA3 Secure',
    isRegistered: true,
    capacityMax: 100,
    capacityActive: 0,
    billingRate: billingRate || 'Free',
    description: description || 'High-performance interlinked gateway node.'
  };

  if (useLocalMemoryDb) {
    const exists = memoryRouters.some(r => r.bssid.toLowerCase() === bssid.toLowerCase());
    if (exists) {
      return res.status(400).json({ success: false, message: 'Mac address hardware Node is already registered.' });
    }
    memoryRouters.push(newRouter);
    return res.status(201).json({ success: true, message: 'Router node certified successfully.', data: newRouter });
  } else {
    try {
      const existsQuery = await dbPool.query('SELECT bssid FROM routers WHERE bssid = $1', [bssid]);
      if (existsQuery.rows.length > 0) {
        return res.status(400).json({ success: false, message: 'Mac address hardware Node is already registered.' });
      }

      await dbPool.query(
        `INSERT INTO routers (bssid, ssid, download_speed_mbps, upload_speed_mbps, security_type, billing_rate, description, owner_id) 
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8)`,
        [bssid, ssid, parseFloat(downloadSpeedMbps), parseFloat(uploadSpeedMbps), newRouter.securityType, newRouter.billingRate, newRouter.description, req.user.id]
      );

      return res.status(201).json({ success: true, message: 'Router node certified successfully.', data: newRouter });
    } catch (e) {
      console.error(e);
      return res.status(500).json({ success: false, message: 'Failed to deploy router node parameters.' });
    }
  }
});

// Endpoint 5: Delete/De-register Router Node (Secured + Admin Only)
app.delete('/api/routers/:bssid', authenticateToken, async (req, res) => {
  const { bssid } = req.params;

  if (useLocalMemoryDb) {
    const idx = memoryRouters.findIndex(r => r.bssid.toLowerCase() === bssid.toLowerCase());
    if (idx === -1) {
      return res.status(404).json({ success: false, message: 'Router node hardware not found.' });
    }
    memoryRouters.splice(idx, 1);
    return res.json({ success: true, message: 'Router node de-certified successfully.' });
  } else {
    try {
      const result = await dbPool.query('DELETE FROM routers WHERE bssid = $1', [bssid]);
      if (result.rowCount === 0) {
        return res.status(404).json({ success: false, message: 'Router node hardware not found.' });
      }
      return res.json({ success: true, message: 'Router node de-certified successfully.' });
    } catch (e) {
      console.error(e);
      return res.status(500).json({ success: false, message: 'Failed to delete router node.' });
    }
  }
});

// Boot listening port server
app.listen(PORT, () => {
  console.log(`📡 SignalConnect backend server is live on listening port ${PORT}`);
});
