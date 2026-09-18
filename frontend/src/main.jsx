import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, useLocation, useNavigate, Routes, Route, Navigate } from 'react-router-dom';
import {
  LayoutDashboard, FilePlus2, ClipboardList, CreditCard, Truck, Star, Users, Package, Clock3,
  Download, LogOut, Bell, Search, CheckCircle2, XCircle, ChevronRight, Menu, ShieldCheck,
  Building2, Send, MapPin, Mail, UserCircle2, RefreshCw, AlertCircle, FileSpreadsheet, BarChart3, Receipt, Filter, PackageCheck, Plus, Image as ImageIcon, Boxes, Pencil
} from 'lucide-react';
import './styles.css';
import api, { unwrap } from './api/api';

const TOKEN_KEY = 'procurement_token';
const USER_KEY = 'procurement_user';

function apiError(error) {
  return error?.response?.data?.message || error?.message || 'Something went wrong.';
}
function money(v) {
  const n = Number(v || 0);
  return `₹${n.toLocaleString('en-IN')}`;
}
function roleLabel(role) {
  if (role === 'EMPLOYEE') return 'USER';
  return role || 'USER';
}
function currentUser() {
  try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null'); } catch { return null; }
}
function saveAuth(auth) {
  localStorage.setItem(TOKEN_KEY, auth.token);
  localStorage.setItem(USER_KEY, JSON.stringify(auth));
}
function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

function App() {
  return <Routes>
    <Route path="/" element={<Login />} />
    <Route path="/app/*" element={<Portal />} />
  </Routes>;
}

function Login() {
  const nav = useNavigate();
  const [mode, setMode] = useState('login');
  const [role, setRole] = useState('EMPLOYEE');
  const [form, setForm] = useState({ username: '', password: '', email: '', fullName: '', departmentId: '', supplierId: '' });
  const [departments, setDepartments] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/departments').then(r => setDepartments(unwrap(r) || [])).catch(() => {});
    api.get('/suppliers').then(r => setSuppliers(unwrap(r) || [])).catch(() => {});
  }, []);

  const submit = async (e) => {
    e.preventDefault(); setBusy(true); setError('');
    try {
      if (mode === 'login') {
        const auth = unwrap(await api.post('/auth/login', { username: form.username, password: form.password, role }));
        saveAuth(auth);
        nav(auth.role === 'SUPPLIER' ? '/app/supplier-dashboard' : '/app/dashboard', { replace: true });
      } else {
        const auth = unwrap(await api.post('/auth/register', {
          username: form.username,
          password: form.password,
          email: form.email,
          fullName: form.fullName,
          role,
          departmentId: form.departmentId ? Number(form.departmentId) : null,
          supplierId: role === 'SUPPLIER' && form.supplierId ? Number(form.supplierId) : null
        }));
        saveAuth(auth);
        nav(auth.role === 'SUPPLIER' ? '/app/supplier-dashboard' : '/app/dashboard', { replace: true });
      }
    } catch (err) { setError(apiError(err)); }
    finally { setBusy(false); }
  };

  return <div className="auth">
    <div className="auth-left"><div className="brand big"><span className="logo">P</span> ProcureFlow</div>
      <div className="hero"><p className="eyebrow">SMART PROCUREMENT MANAGEMENT</p><h1>From request to delivery,<br/><em>all in one flow.</em></h1>
      <p>Manage purchase requests, approvals, supplier payment details, shipments and ratings from one clean workspace.</p>
      <div className="flow"><span>Request</span><b>→</b><span>Approve</span><b>→</b><span>Pay</span><b>→</b><span>Track</span></div></div>
    </div>
    <form className="auth-card" onSubmit={submit}>
      <div className="auth-tabs"><button type="button" className={mode==='login'?'active':''} onClick={()=>{setMode('login');setError('')}}>Sign in</button><button type="button" className={mode==='register'?'active':''} onClick={()=>{setMode('register');setError('')}}>Register</button></div>
      <h2>{mode==='login'?'Welcome back':'Create account'}</h2><p className="muted">{mode==='login'?'Enter your details to access the portal.':'Create a backend account for the procurement portal.'}</p>
      {mode==='register' && <Field label="Full name" value={form.fullName} onChange={v=>setForm({...form,fullName:v})} placeholder="Your full name"/>}
      <Field label="Username" value={form.username} onChange={v=>setForm({...form,username:v})} placeholder="username"/>
      {mode==='register' && <Field label="Email" value={form.email} onChange={v=>setForm({...form,email:v})} placeholder="name@company.com"/>}
      <Field label="Password" value={form.password} onChange={v=>setForm({...form,password:v})} placeholder="••••••••" type="password"/>
      {mode==='register' && <><label className="label">Department</label><select className="input" value={form.departmentId} onChange={e=>setForm({...form,departmentId:e.target.value})}><option value="">Select department</option>{departments.map(d=><option key={d.id} value={d.id}>{d.name} ({d.code})</option>)}</select></>}
      <label className="label">Login / account type</label><div className="role-grid">{['EMPLOYEE','ADMIN','SUPPLIER'].map(r=><button type="button" key={r} className={role===r?'role active':'role'} onClick={()=>setRole(r)}>{r==='EMPLOYEE'?<UserCircle2 size={18}/>:r==='ADMIN'?<ShieldCheck size={18}/>:<Building2 size={18}/>} {r==='EMPLOYEE'?'USER':r}</button>)}</div>
      {mode==='register' && role==='SUPPLIER' && <><label className="label">Supplier company</label><select required className="input" value={form.supplierId} onChange={e=>setForm({...form,supplierId:e.target.value})}><option value="">Select your supplier record</option>{suppliers.map(s=><option key={s.id} value={s.id}>{s.name} — #{s.id}</option>)}</select><p className="tiny">This links the login to the existing supplier record.</p></>}
      {error && <div className="error"><AlertCircle size={16}/>{error}</div>}
      <button className="primary wide" disabled={busy}>{busy?<RefreshCw className="spin" size={18}/>:<ChevronRight size={18}/>} {busy?'Please wait…':mode==='login'?'Sign in':'Create account'}</button>
      <p className="tiny">Connected to Spring Boot at http://localhost:8081</p>
    </form>
  </div>;
}

function Field({label, placeholder, type='text', value, onChange, required=false}) {
  return <><label className="label">{label}</label><input required={required} className="input" placeholder={placeholder} type={type} value={value ?? ''} onChange={e=>onChange?.(e.target.value)}/></>;
}

function Portal() {
  const nav = useNavigate(), loc = useLocation();
  const [mobile,setMobile] = useState(false);
  const [me,setMe] = useState(currentUser());
  const path = loc.pathname.split('/')[2] || 'dashboard';
  const isAdmin = me?.role === 'ADMIN';
  const isSupplier = me?.role === 'SUPPLIER';
  const navs = isSupplier
    ? [['supplier-dashboard','Supplier Dashboard',LayoutDashboard],['supplier-products','My Products',Package],['supplier-ratings','User Ratings',Star],['supplier-payments','Payments',CreditCard],['supplier-shipments','Shipments',Truck],['supplier-reports','Payment Reports',FileSpreadsheet]]
    : isAdmin
    ? [['dashboard','Dashboard',LayoutDashboard],['product-board','Product Board',Package],['requests','Approvals',ClipboardList],['users','Users',Users],['suppliers','Suppliers',Building2],['ratings','Ratings',Star],['reports','CSV Reports',FileSpreadsheet]]
    : [['dashboard','Dashboard',LayoutDashboard],['product-board','Product Board',Package],['new-request','New Request',FilePlus2],['requests','My Requests',ClipboardList],['payments','Payments',CreditCard],['tracking','Tracking',Truck],['rating','Rate Supplier',Star]];

  useEffect(()=>{
    if (!localStorage.getItem(TOKEN_KEY)) nav('/', {replace:true});
    else api.get('/users/me').then(r=>{const u=unwrap(r); const auth={...(JSON.parse(localStorage.getItem(USER_KEY)||'{}')), fullname:u.fullName, username:u.username, role:u.role, supplierId:u.supplierId}; localStorage.setItem(USER_KEY,JSON.stringify(auth)); setMe(auth);}).catch(()=>{});
  },[]);

  const logout = ()=>{clearAuth();nav('/',{replace:true});};
  return <div className="shell"><aside className={mobile?'open':''}><div className="brand"><span className="logo">P</span> ProcureFlow</div><div className="side-label">WORKSPACE</div>
    {navs.map(([key,label,I])=><button key={key} className={path===key?'nav active':'nav'} onClick={()=>{nav('/app/'+key);setMobile(false)}}><I size={18}/>{label}</button>)}
    <div className="side-spacer"/><div className="demo-switch"><span>Signed in as</span><b>{roleLabel(me?.role)}</b></div>
    <button className="nav logout" onClick={logout}><LogOut size={18}/>Sign out</button>
  </aside><main><header><button className="icon-btn mobile-menu" onClick={()=>setMobile(!mobile)}><Menu/></button><div><div className="crumb">Portal / {navs.find(x=>x[0]===path)?.[1]||'Supplier Console'}</div><h2>{navs.find(x=>x[0]===path)?.[1]||'Supplier Console'}</h2></div><div className="header-actions"><button className="icon-btn"><Bell size={19}/></button><div className="avatar">{(me?.fullname||'U').slice(0,2).toUpperCase()}</div><div className="user-meta"><b>{me?.fullname || me?.username || 'User'}</b><span>{roleLabel(me?.role)}</span></div></div></header>
    <div className="content"><Routes>
      <Route path="dashboard" element={isSupplier?<Navigate to="../supplier-dashboard" replace/>:<Dashboard me={me}/>}/>
      <Route path="supplier-dashboard" element={<SupplierDashboard me={me}/>}/>
      <Route path="supplier-products" element={<SupplierProducts me={me}/>}/><Route path="supplier-ratings" element={<SupplierRatings me={me}/>}/>
      <Route path="supplier-payments" element={<SupplierPayments me={me}/>}/>
      <Route path="supplier-shipments" element={<SupplierShipments me={me}/>}/>
      <Route path="supplier-reports" element={<SupplierReports me={me}/>}/><Route path="product-board" element={<ProductBoard/>}/><Route path="new-request" element={<NewRequest/>}/><Route path="requests" element={<Requests isAdmin={isAdmin}/>}/>
      <Route path="payments" element={<Payments/>}/><Route path="tracking" element={<Tracking/>}/><Route path="rating" element={<Rate/>}/>
      <Route path="users" element={<UsersPage/>}/><Route path="products" element={<Products/>}/><Route path="suppliers" element={<Suppliers/>}/><Route path="ratings" element={<Ratings/>}/><Route path="reports" element={<AdminReports/>}/>
      <Route path="supplier-console" element={<SupplierConsole/>}/><Route path="*" element={<Navigate to="dashboard"/>}/>
    </Routes></div></main></div>;
}

function Stat({icon:I,label,value,sub}){return <div className="stat"><div className="stat-icon"><I size={20}/></div><div><span>{label}</span><strong>{value}</strong><small>{sub}</small></div></div>}
function Dashboard({me}){
  const [data,setData]=useState([]); const [payments,setPayments]=useState([]); const [shipments,setShipments]=useState([]); const [loading,setLoading]=useState(true);
  const isAdmin=me?.role==='ADMIN';
  const load=async()=>{setLoading(true);try{
    const [r,p,t]=await Promise.all([api.get(isAdmin?'/purchase-requests':'/purchase-requests/my'),api.get(isAdmin?'/payments':'/payments/my'),api.get('/tracking/my')]);
    setData(unwrap(r)||[]);setPayments(unwrap(p)||[]);setShipments(unwrap(t)||[]);
  }catch{}finally{setLoading(false)}};
  useEffect(()=>{load()},[isAdmin]);
  const pending=data.filter(x=>x.status==='PENDING_APPROVAL').length;
  const approved=data.filter(x=>x.status==='APPROVED').length;
  const rejected=data.filter(x=>x.status==='REJECTED').length;
  const unpaid=payments.filter(x=>x.status!=='PAID').length;
  const paid=payments.filter(x=>x.status==='PAID').length;
  const delivered=shipments.filter(x=>x.status==='DELIVERED').length;
  const inTransit=shipments.filter(x=>x.status==='IN_TRANSIT').length;
  const totalSpend=payments.reduce((a,p)=>a+Number(p.amount||0),0);
  const max=Math.max(approved,pending,rejected,1);
  return <div>
    <div className="welcome"><div><p className="eyebrow">PROCUREMENT WORKSPACE</p><h1>Good afternoon, {me?.fullname?.split(' ')[0] || me?.username || 'there'} 👋</h1><p className="muted">Monitor requests, payments and delivery progress from one place.</p></div><button className="secondary" onClick={load}><RefreshCw size={15}/> Refresh dashboard</button></div>
    <div className="stats">
      <Stat icon={ClipboardList} label={isAdmin?'Total requests':'Active requests'} value={loading?'…':data.length} sub={isAdmin?`${pending} awaiting approval`:'From your account'}/>
      <Stat icon={CreditCard} label="Pending payments" value={loading?'…':unpaid} sub={`${paid} completed`}/>
      <Stat icon={Truck} label="Shipments" value={loading?'…':shipments.length} sub={`${inTransit} in transit · ${delivered} delivered`}/>
      <Stat icon={Receipt} label={isAdmin?'Procurement value':'Paid value'} value={loading?'…':money(totalSpend)} sub={isAdmin?'Across payment records':'Your payment records'}/>
    </div>
    {isAdmin&&<div className="analytics-grid">
      <Panel title="Request status overview"><div className="bar-chart">
        {[['Pending approval',pending,'orange'],['Approved',approved,'green'],['Rejected',rejected,'red']].map(([label,val,cls])=><div className="bar-row" key={label}><div><span>{label}</span><b>{val}</b></div><div className="bar-track"><span className={cls} style={{width:`${Math.max((val/max)*100, val?6:0)}%`}}/></div></div>)}
      </div></Panel>
      <Panel title="Procurement health"><div className="health-grid"><div><span>Paid</span><strong>{paid}</strong></div><div><span>Pending payment</span><strong>{unpaid}</strong></div><div><span>In transit</span><strong>{inTransit}</strong></div><div><span>Delivered</span><strong>{delivered}</strong></div></div><div className="analytics-note"><BarChart3 size={16}/> Keep approvals, payments and shipment statuses up to date for accurate reporting.</div></Panel>
    </div>}
    <div className="grid-2"><Panel title={isAdmin?'Recent procurement requests':'Recent requests'}><div className="table-wrap"><table><thead><tr><th>Request</th><th>Status</th><th>Amount</th></tr></thead><tbody>{loading?<tr><td colSpan="3">Loading…</td></tr>:data.slice().sort((a,b)=>(b.id||0)-(a.id||0)).slice(0,6).map(r=><tr key={r.id}><td><b>#{r.id} · {r.title}</b><span className="subcell">{r.requester?.fullName||r.requester?.username||'My request'}</span></td><td><Status s={r.status}/></td><td>{money(r.totalAmount)}</td></tr>)}{!loading&&!data.length&&<tr><td colSpan="3">No requests yet.</td></tr>}</tbody></table></div></Panel><Panel title="Procurement timeline"><Timeline requests={data}/></Panel></div>
  </div>;
}
function Timeline({requests=[]}){return <div className="timeline">{requests.slice(0,4).map((r,i)=><div className="time" key={r.id||i}><div className={'dot '+(r.status==='APPROVED'?'green':'')}>{r.status==='APPROVED'?<CheckCircle2 size={14}/>:<Clock3 size={14}/>}</div><div><b>#{r.id} · {r.title}</b><span>{r.status?.replaceAll('_',' ')}</span></div></div>)}{!requests.length&&<p className="muted">No requests yet.</p>}</div>}
function Panel({title,action,children}){return <section className="panel"><div className="panel-head"><h3>{title}</h3>{action&&<button className="text-btn">{action} <ChevronRight size={15}/></button>}</div>{children}</section>}
function Status({s}){let cls=s?.includes('APPROV')?'orange':s==='PAID'||s==='APPROVED'||s==='DELIVERED'?'green':s==='REJECTED'?'red':'blue';return <span className={'status '+cls}>{String(s||'—').replaceAll('_',' ')}</span>}

function NewRequest(){
  const nav=useNavigate(); const [products,setProducts]=useState([]); const [departments,setDepartments]=useState([]); const [departmentId,setDepartmentId]=useState(''); const [productId,setProductId]=useState(''); const [qty,setQty]=useState(1); const [saved,setSaved]=useState(false); const [error,setError]=useState(''); const [busy,setBusy]=useState(false);
  useEffect(()=>{Promise.all([api.get('/products'),api.get('/departments')]).then(([p,d])=>{setProducts(unwrap(p)||[]);setDepartments(unwrap(d)||[])}).catch(e=>setError(apiError(e)))},[]);
  const selected=products.find(p=>String(p.id)===String(productId)); const total=selected?Number(selected.price)*Number(qty||0):0;
  const submit=async(e)=>{e.preventDefault();setBusy(true);setError('');try{const body={title:selected?`${qty} ${selected.name}`:'Purchase request',description:'',totalAmount:total,status:'DRAFT',department:departmentId?{id:Number(departmentId)}:undefined,items:selected?[{product:{id:selected.id},quantity:Number(qty),price:selected.price,totalPrice:total}]:[]};const created=unwrap(await api.post('/purchase-requests',body));await api.post(`/purchase-requests/${created.id}/submit`);setSaved(true);setTimeout(()=>nav('/app/requests'),700)}catch(err){setError(apiError(err))}finally{setBusy(false)}};
  return <div><div className="page-intro"><p className="eyebrow">PURCHASE REQUEST</p><h1>New purchase request</h1><p className="muted">Select the department, product and quantity for your purchase.</p></div><form className="panel form-card" onSubmit={submit}><div className="form-grid"><div><label className="label">Department</label><select required className="input" value={departmentId} onChange={e=>setDepartmentId(e.target.value)}><option value="">Select department</option>{departments.map(d=><option key={d.id} value={d.id}>{d.name}</option>)}</select></div><div><label className="label">Product</label><select required className="input" value={productId} onChange={e=>setProductId(e.target.value)}><option value="">Select product</option>{products.map(p=><option key={p.id} value={p.id}>{p.name} · {money(p.price)}</option>)}</select></div><div><Field label="Quantity" type="number" placeholder="1" value={qty} onChange={setQty} required/></div></div>{selected&&<div className="success">Selected: <b>{selected.name}</b> × {qty} = <b>{money(total)}</b></div>}{error&&<div className="error"><AlertCircle size={16}/>{error}</div>}<div className="form-actions"><button type="button" className="secondary" onClick={()=>nav('/app/dashboard')}>Cancel</button><button className="primary" disabled={busy}>{busy?<RefreshCw className="spin" size={17}/>:<Send size={17}/>} {busy?'Submitting…':'Submit for approval'}</button></div>{saved&&<div className="success"><CheckCircle2 size={18}/> Request submitted successfully.</div>}</form></div>
}

function Requests({isAdmin}){
  const [data,setData]=useState([]);const [error,setError]=useState('');const [loading,setLoading]=useState(true);const [busyId,setBusyId]=useState(null);const [payments,setPayments]=useState([]);const [selectedPayment,setSelectedPayment]=useState(null);const [successPayment,setSuccessPayment]=useState(null);const [query,setQuery]=useState('');const [statusFilter,setStatusFilter]=useState('ALL');
  const load=async()=>{setLoading(true);setError('');try{const r=await api.get(isAdmin?'/purchase-requests/pending':'/purchase-requests/my');setData(unwrap(r)||[]);if(!isAdmin){const p=await api.get('/payments/my');setPayments(unwrap(p)||[]);}}catch(e){setError(apiError(e))}finally{setLoading(false)}};
  useEffect(load,[isAdmin]);
  useEffect(()=>{if(!isAdmin&&payments.length){const approvedIds=new Set(data.filter(r=>r.status==='APPROVED').map(r=>r.id));const p=payments.find(x=>approvedIds.has(x.purchaseRequest?.id)&&x.status!=='PAID');if(p)setSelectedPayment(p)}},[data,payments,isAdmin]);
  const act=async(id,action)=>{if(action==='reject'&&!window.confirm(`Reject purchase request #${id}?`))return;setBusyId(id);setError('');try{await api.post(`/purchase-requests/${id}/${action}`);await load()}catch(e){setError(apiError(e))}finally{setBusyId(null)}};
  const filtered=data.filter(r=>{const text=`${r.id||''} ${r.title||''} ${r.requester?.fullName||''} ${r.requester?.email||''} ${r.department?.name||''}`.toLowerCase();return text.includes(query.toLowerCase())&&(statusFilter==='ALL'||r.status===statusFilter)});
  return <div><div className="page-intro"><p className="eyebrow">{isAdmin?'ADMIN QUEUE':'MY PROCUREMENT'}</p><h1>{isAdmin?'Approval requests':'My purchase requests'}</h1><p className="muted">{isAdmin?'Review purchase requests and keep approval decisions up to date.':'Track requests and pay approved orders from here.'}</p></div>{error&&<div className="error"><AlertCircle size={16}/>{error}</div>}<Panel title={isAdmin?'Requests awaiting approval':'All requests'}><div className="filters"><div className="search"><Search size={17}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Search by request, user or department..."/></div><select value={statusFilter} onChange={e=>setStatusFilter(e.target.value)}><option value="ALL">All statuses</option><option value="PENDING_APPROVAL">Pending</option><option value="APPROVED">Approved</option><option value="REJECTED">Rejected</option></select><button className="secondary" onClick={load}><RefreshCw size={16}/> Refresh</button></div><div className="filter-summary"><Filter size={14}/> Showing {filtered.length} of {data.length} requests</div><div className="table-wrap"><table><thead><tr><th>Request</th><th>Requester</th><th>Department</th><th>Amount</th><th>Status</th><th>Action</th></tr></thead><tbody>{loading?<tr><td colSpan="6">Loading…</td></tr>:filtered.map(r=><tr key={r.id}><td><b>#{r.id} · {r.title}</b><span className="subcell">{r.items?.length||0} items</span></td><td>{r.requester?.fullName||r.requester?.username||'—'}<span className="subcell">{r.requester?.email||''}</span></td><td>{r.department?.name||'—'}</td><td>{money(r.totalAmount)}</td><td><Status s={r.status}/></td><td>{isAdmin&&r.status==='PENDING_APPROVAL'?<div className="row-actions"><button className="approve" disabled={busyId===r.id} onClick={()=>act(r.id,'approve')}><CheckCircle2 size={15}/> {busyId===r.id?'Processing…':'Approve'}</button><button className="reject" disabled={busyId===r.id} onClick={()=>act(r.id,'reject')}><XCircle size={15}/> Reject</button></div>:!isAdmin&&r.status==='APPROVED'?(payments.find(x=>x.purchaseRequest?.id===r.id)?.status==='PAID'?<button className="payment-done-btn" disabled><CheckCircle2 size={15}/> Payment done successfully</button>:<button className="primary" onClick={()=>{const p=payments.find(x=>x.purchaseRequest?.id===r.id);if(p)setSelectedPayment(p)}}><CreditCard size={14}/> Pay now</button>):<span className="muted">—</span>}</td></tr>)}{!loading&&!filtered.length&&<tr><td colSpan="6">No matching requests found.</td></tr>}</tbody></table></div></Panel>{selectedPayment&&<PaymentModal payment={selectedPayment} onClose={()=>setSelectedPayment(null)} onPaid={p=>{setSelectedPayment(null);setPayments(prev=>prev.map(x=>x.id===p.id?p:x));load();setSuccessPayment(p)}}/>}{successPayment&&<PaymentSuccessModal payment={successPayment} onClose={()=>setSuccessPayment(null)}/>}</div>
}

function PaymentSuccessModal({payment,onClose}){
  const nav=useNavigate();
  const request=payment?.purchaseRequest;
  const productName=request?.title||'Purchase request';
  const transactionId=payment?.transactionReference||`TXN-${payment?.id||Date.now()}`;
  const paidAt=payment?.paidAt?String(payment.paidAt).replace('T',' '):new Date().toLocaleString();
  const items=request?.items?.reduce((n,i)=>n+Number(i.quantity||0),0)||request?.items?.length||1;
  return <div className="payment-success-overlay">
    <div className="payment-success-modal">
      <button className="payment-success-close" onClick={onClose}>×</button>
      <div className="payment-success-icon"><CheckCircle2 size={48}/></div>
      <h2>PAYMENT SUCCESSFUL!</h2>
      <p className="payment-success-subtitle">Thank you for your purchase. Your order has been successfully placed and is being prepared for shipment.</p>
      <div className="payment-success-summary">
        <h3>Order Summary</h3>
        <div className="payment-success-row"><span>Product</span><b>{productName}</b></div>
        <div className="payment-success-row"><span>Transaction ID</span><b>{transactionId}</b></div>
        <div className="payment-success-row"><span>Ordered</span><b>{items} {items===1?'Item':'Items'}</b></div>
        <div className="payment-success-row"><span>Payment Method</span><b>{payment?.paymentMethod||'Online Payment'}</b></div>
        <div className="payment-success-row"><span>Paid At</span><b>{paidAt}</b></div>
        <div className="payment-success-row payment-success-total"><span>TOTAL</span><b>{money(payment?.amount)}</b></div>
      </div>
      <button className="payment-success-home" onClick={()=>{onClose();nav('/app/dashboard')}}>GO TO HOME</button>
      <button className="payment-success-track" onClick={()=>{onClose();nav(`/app/tracking?requestId=${payment?.purchaseRequest?.id||''}`)}}>TRACK ORDER</button>
    </div>
  </div>;
}

function PaymentModal({payment,onClose,onPaid}){
  const [method,setMethod]=useState('UPI');const [card,setCard]=useState({holder:'',number:'',expiry:'',cvv:''});const [busy,setBusy]=useState(false);const [error,setError]=useState('');
  const upi=payment?.upiId||payment?.supplier?.upiId||'';const upiUri=upi?`upi://pay?pa=${encodeURIComponent(upi)}&pn=${encodeURIComponent(payment.supplier?.name||'Supplier')}&am=${encodeURIComponent(payment.amount||0)}&cu=INR`:'';const qr=upiUri?`https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(upiUri)}`:'';
  const pay=async()=>{setBusy(true);setError('');try{if(method==='CARD' && (!/^\d{16}$/.test(card.number.replace(/\s/g,''))||!/^\d{3,4}$/.test(card.cvv)||!/^\d{2}\/\d{2}$/.test(card.expiry))){throw new Error('Enter valid card number, expiry (MM/YY) and CVV.')}const body={paymentMethod:method,cardLast4:method==='CARD'?card.number.replace(/\s/g,'').slice(-4):null,transactionReference:`TXN-${Date.now()}`};const r=await api.post(`/payments/${payment.purchaseRequest?.id}/mark-paid`,body);onPaid?.(unwrap(r));}catch(e){setError(apiError(e))}finally{setBusy(false)}};
  return <div className="modal-backdrop"><div className="payment-modal"><div className="modal-head"><div><span className="small">SECURE PAYMENT · REQUEST #{payment.purchaseRequest?.id}</span><h2>Pay {money(payment.amount)}</h2><p className="muted">{payment.supplier?.name||'Supplier'} · {payment.purchaseRequest?.title||'Purchase request'}</p></div><button className="icon-btn" onClick={onClose}>×</button></div>{error&&<div className="error"><AlertCircle size={16}/>{error}</div>}<div className="pay-methods"><button className={method==='UPI'?'method active':'method'} onClick={()=>setMethod('UPI')}><CreditCard size={18}/> UPI / Scanner</button><button className={method==='CARD'?'method active':'method'} onClick={()=>setMethod('CARD')}><CreditCard size={18}/> Credit / Debit Card</button></div>{method==='UPI'?<div className="upi-pay"><div><h3>Scan & pay</h3><p className="muted">Scan this QR with any UPI app or use the UPI ID below.</p>{upi?<><div className="upi-id">{upi}</div><a className="secondary" href={upiUri}>Open UPI app</a></>:<div className="error">Supplier UPI ID is not configured.</div>}</div>{qr&&<img className="upi-qr" src={qr} alt="UPI payment QR code"/>}</div>:<div className="card-form"><Field label="Card holder name" placeholder="Name on card" value={card.holder} onChange={v=>setCard({...card,holder:v})}/><Field label="Card number" placeholder="1234 5678 9012 3456" value={card.number} onChange={v=>setCard({...card,number:v})}/><div className="form-grid"><Field label="Expiry" placeholder="MM/YY" value={card.expiry} onChange={v=>setCard({...card,expiry:v})}/><Field label="CVV" placeholder="123" type="password" value={card.cvv} onChange={v=>setCard({...card,cvv:v})}/></div><p className="tiny">For safety, the backend stores only the card's last 4 digits. Use a real payment gateway for production card processing.</p></div>}{payment.status==='PAID'?<div className="paid-note"><CheckCircle2 size={17}/> Payment already completed.</div>:<button className="primary wide" disabled={busy||(method==='UPI'&&!upi)} onClick={pay}>{busy?'Processing…':`Pay ${money(payment.amount)}`}</button>}</div></div>;
}

function printReceipt(p){
  const w=window.open('','_blank','width=720,height=760'); if(!w) return;
  w.document.write(`<html><head><title>Payment Receipt #${p.purchaseRequest?.id||''}</title><style>body{font-family:Arial;padding:40px;color:#18212f}h1{margin-bottom:5px}.muted{color:#66748a}.box{border:1px solid #ddd;padding:18px;border-radius:10px;margin-top:20px}.row{display:flex;justify-content:space-between;padding:9px 0;border-bottom:1px solid #eee}.total{font-size:22px;font-weight:bold}</style></head><body><h1>Payment Receipt</h1><p class="muted">ProcureFlow · Request #${p.purchaseRequest?.id||'—'}</p><div class="box"><div class="row"><b>Supplier</b><span>${p.supplier?.name||'—'}</span></div><div class="row"><b>Product / Request</b><span>${p.purchaseRequest?.title||'—'}</span></div><div class="row"><b>Payment Method</b><span>${p.paymentMethod||'—'}</span></div><div class="row"><b>Transaction ID</b><span>${p.transactionReference||'—'}</span></div><div class="row"><b>Paid At</b><span>${p.paidAt?.replace('T',' ')||'—'}</span></div><div class="row total"><b>Total Paid</b><span>${money(p.amount)}</span></div></div><script>window.print();</script></body></html>`); w.document.close();
}

function Payments(){
  const [data,setData]=useState([]);const [error,setError]=useState('');const [selected,setSelected]=useState(null);
  const load=()=>api.get('/payments/my').then(r=>setData(unwrap(r)||[])).catch(e=>setError(apiError(e)));useEffect(load,[]);
  const download=async()=>{try{const r=await api.get('/payments/export/my',{responseType:'blob'});const url=URL.createObjectURL(r.data);const a=document.createElement('a');a.href=url;a.download='my-payments.csv';a.click();URL.revokeObjectURL(url)}catch(e){setError(apiError(e))}};
  return <div><div className="page-intro"><p className="eyebrow">PAYMENTS</p><h1>Payment center</h1><p className="muted">Approved requests show the supplier's UPI details and scanner, or secure card payment.</p></div>{error&&<div className="error"><AlertCircle size={16}/>{error}</div>}<div className="payment-grid">{data.map(p=><div className="payment-card" key={p.id}><div className="pc-head"><div><span className="small">REQUEST #{p.purchaseRequest?.id}</span><h3>{p.supplier?.name||'Supplier'}</h3></div><Status s={p.status}/></div><div className="amount">{money(p.amount)}</div><div className="bank-grid"><span>UPI ID<strong>{p.upiId||p.supplier?.upiId||'Not configured'}</strong></span><span>Supplier ID<strong>#{p.supplier?.id||'—'}</strong></span><span>Payment method<strong>{p.paymentMethod||'—'}</strong></span><span>Paid at<strong>{p.paidAt?.replace('T',' ')||'Pending'}</strong></span></div>{p.status!=='PAID'?<button className="primary wide" onClick={()=>setSelected(p)}><CreditCard size={17}/> Open payment page</button>:<div className="paid-note"><CheckCircle2 size={17}/> Payment confirmed · Supplier notified <button className="receipt-btn" onClick={()=>printReceipt(p)}> <Receipt size={13}/> Receipt</button></div>}</div>)}{!data.length&&<div className="panel"><p className="muted">No payment records yet. Payment is created automatically when an admin approves a request.</p></div>}</div><button className="secondary download" onClick={download}><Download size={17}/> Download my payment CSV</button>{selected&&<PaymentModal payment={selected} onClose={()=>setSelected(null)} onPaid={p=>{setSelected(null);setData(prev=>prev.map(x=>x.id===p.id?p:x));load()}}/>}</div>}

function Tracking(){
  const [data,setData]=useState([]);const [error,setError]=useState('');const [query,setQuery]=useState('');const [loading,setLoading]=useState(true);
  const load=async()=>{
    setLoading(true);setError('');
    try{
      // A paid order must be visible here even when the supplier has not created
      // a shipment record yet. In that case we show NOT SHIPPED.
      const payments=unwrap(await api.get('/payments/my'))||[];
      const paid=payments.filter(p=>p.status==='PAID');
      const shipments=unwrap(await api.get('/tracking/my'))||[];
      const byRequest=new Map(shipments.map(s=>[String(s.purchaseRequest?.id),s]));
      const merged=paid.map(p=>{
        const requestId=p.purchaseRequest?.id;
        const shipment=byRequest.get(String(requestId));
        if(shipment){
          return {...shipment,payment:p};
        }
        return {
          id:`payment-${p.id}`,
          status:'NOT_SHIPPED',
          purchaseRequest:p.purchaseRequest,
          payment:p,
          trackingNumber:null,
          courierName:null,
          currentLocation:null,
          trackingUrl:null,
          history:[]
        };
      });
      setData(merged);
    }catch(e){setError(apiError(e))}finally{setLoading(false)}
  };
  useEffect(load,[]);
  const filtered=data.filter(s=>`${s.purchaseRequest?.id||''} ${s.purchaseRequest?.title||''} ${s.trackingNumber||''} ${s.courierName||''} ${s.currentLocation||''} ${s.payment?.transactionReference||''}`.toLowerCase().includes(query.toLowerCase()));
  const steps=['READY_TO_SHIP','SHIPPED','IN_TRANSIT','DELIVERED'];
  const paymentMethod=p=>p?.paymentMethod||'Online Payment';
  return <div><div className="page-intro"><p className="eyebrow">SHIPMENT TRACKING</p><h1>Track your deliveries</h1><p className="muted">After payment, your order appears here immediately. If the supplier has not shipped it yet, the status will show <b>NOT SHIPPED</b>.</p></div>{error&&<div className="error"><AlertCircle size={16}/>{error}</div>}<div className="filters"><div className="search"><Search size={17}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Search request, product, tracking number or transaction ID..."/></div><button className="secondary" onClick={load}><RefreshCw size={16}/> Refresh</button></div>{loading?<div className="panel"><p className="muted">Loading payment and tracking details…</p></div>:filtered.map(s=>{const idx=steps.indexOf(s.status);const p=s.payment;return <div className="track-card" key={s.id}><div className="track-head"><div><span className="small">REQUEST #{s.purchaseRequest?.id}</span><h3>{s.purchaseRequest?.title||'Purchase request'}</h3><span className="subcell">{s.courierName?s.courierName+' · ':''}{s.trackingNumber||'Tracking not assigned yet'}</span></div><Status s={s.status}/></div><div className="tracking-payment-box"><div><span>Payment status</span><b className="paid-text">{p?.status||'PAID'}</b></div><div><span>Amount paid</span><b>{money(p?.amount)}</b></div><div><span>Transaction ID</span><b>{p?.transactionReference||'—'}</b></div><div><span>Payment method</span><b>{paymentMethod(p)}</b></div><div><span>Paid at</span><b>{p?.paidAt?String(p.paidAt).replace('T',' '):'—'}</b></div></div>{s.status==='NOT_SHIPPED'?<div className="not-shipped-banner"><Package size={20}/><div><b>NOT SHIPPED</b><span>Payment is successful, but the supplier has not shipped this order yet.</span></div></div>:<><div className="track-progress">{steps.map((step,i)=><span key={step} className={i<=idx?'active':''}><PackageCheck size={13}/> {step.replaceAll('_',' ')}</span>)}</div><div className="location"><MapPin size={18}/><div><b>Current location</b><span>{s.currentLocation||'Awaiting supplier location update'}</span></div>{s.trackingUrl&&<a href={s.trackingUrl} target="_blank" rel="noreferrer">Track on courier →</a>}</div><div className="history">{(s.history||[]).slice().reverse().map((h,i)=><div key={i}><b>{h.eventTime?.replace('T',' ')||'Event'}</b><span>{String(h.status||'').replaceAll('_',' ')} · {h.note||'Status updated'}</span></div>)}</div></>}</div>})}{!loading&&!filtered.length&&<div className="panel"><p className="muted">No paid orders match your search.</p></div>}</div>
}

function Rate(){const [requests,setRequests]=useState([]);const [id,setId]=useState('');const [rating,setRating]=useState(0);const [comment,setComment]=useState('');const [done,setDone]=useState(false);const [error,setError]=useState('');useEffect(()=>{api.get('/purchase-requests/my').then(r=>setRequests((unwrap(r)||[]).filter(x=>x.status==='APPROVED'))).catch(()=>{})},[]);const submit=async()=>{try{await api.post('/ratings',{purchaseRequestId:Number(id),ratingValue:rating,comment});setDone(true)}catch(e){setError(apiError(e))}};return <div className="center-page"><div className="rating-box"><div className="rating-icon"><Star/></div><p className="eyebrow">FEEDBACK</p><h1>Rate your supplier</h1><p className="muted">Submit a rating against a purchase request.</p><label className="label">Purchase request</label><select className="input" value={id} onChange={e=>setId(e.target.value)}><option value="">Select request</option>{requests.map(r=><option key={r.id} value={r.id}>#{r.id} · {r.title}</option>)}</select><div className="stars">{[1,2,3,4,5].map(n=><button type="button" key={n} className={rating>=n?'selected':''} onClick={()=>setRating(n)}><Star fill="currentColor"/></button>)}</div><textarea className="textarea" value={comment} onChange={e=>setComment(e.target.value)} placeholder="Tell us about delivery, quality and service..."/><button className="primary wide" disabled={!rating||!id} onClick={submit}>Submit rating</button>{error&&<div className="error">{error}</div>}{done&&<div className="success">Thank you! Your rating was submitted.</div>}</div></div>}

function AdminReports(){
  const [requests,setRequests]=useState([]);
  const [payments,setPayments]=useState([]);
  const [loading,setLoading]=useState(true);
  const [busy,setBusy]=useState(false);
  const [error,setError]=useState('');

  const load=async()=>{
    setLoading(true);setError('');
    try{
      const [r,p]=await Promise.all([api.get('/purchase-requests'),api.get('/payments')]);
      setRequests(unwrap(r)||[]);setPayments(unwrap(p)||[]);
    }catch(e){setError(apiError(e))}
    finally{setLoading(false)}
  };
  useEffect(()=>{load()},[]);

  const paymentByRequest=useMemo(()=>new Map(payments.map(p=>[p.purchaseRequest?.id,p])),[payments]);
  const approved=requests.filter(r=>r.status==='APPROVED');
  const rejected=requests.filter(r=>r.status==='REJECTED');
  const paid=approved.filter(r=>paymentByRequest.get(r.id)?.status==='PAID');
  const pending=approved.filter(r=>paymentByRequest.get(r.id)?.status!=='PAID');

  const download=async()=>{
    setBusy(true);setError('');
    try{
      const response=await api.get('/purchase-requests/export/admin-report',{responseType:'blob'});
      const url=URL.createObjectURL(response.data);
      const a=document.createElement('a');a.href=url;a.download='procurement-admin-report.csv';
      document.body.appendChild(a);a.click();a.remove();URL.revokeObjectURL(url);
    }catch(e){setError(apiError(e))}finally{setBusy(false)}
  };
  const downloadPaymentHistory=async()=>{
    setBusy(true);setError('');
    try{
      const response=await api.get('/payments/export/all',{responseType:'blob'});
      const url=URL.createObjectURL(response.data);
      const a=document.createElement('a');a.href=url;a.download='all-users-payment-history.csv';
      document.body.appendChild(a);a.click();a.remove();URL.revokeObjectURL(url);
    }catch(e){setError(apiError(e))}finally{setBusy(false)}
  };

  return <div>
    <div className="page-intro"><p className="eyebrow">ADMIN REPORTS</p><h1>Procurement CSV report</h1><p className="muted">Download all users' request decisions and payment status, including approved requests that are still awaiting payment.</p></div>
    {error&&<div className="error"><AlertCircle size={16}/>{error}</div>}
    <div className="stats">
      <Stat icon={ClipboardList} label="Total requests" value={loading?'…':requests.length} sub="All users"/>
      <Stat icon={CheckCircle2} label="Approved" value={loading?'…':approved.length} sub="Approved requests"/>
      <Stat icon={XCircle} label="Rejected" value={loading?'…':rejected.length} sub="Rejected requests"/>
      <Stat icon={CreditCard} label="Payment pending" value={loading?'…':pending.length} sub="Approved but not paid"/>
    </div>
    <Panel title="Report actions">
      <p className="muted">The CSV contains request ID, user, email, department, products, amount, approval status, payment status, supplier and payment timestamps.</p>
      <div className="form-actions" style={{justifyContent:'flex-start'}}>
        <button className="secondary" onClick={load} disabled={loading}><RefreshCw size={16}/> Refresh report</button>
        <button className="primary" onClick={download} disabled={busy}><Download size={16}/> Download procurement CSV</button>
        <button className="secondary" onClick={downloadPaymentHistory} disabled={busy}><Download size={16}/> Download all payment history</button>
      </div>
    </Panel>
    <Panel title="Request and payment summary">
      <div className="table-wrap"><table><thead><tr><th>Request</th><th>User</th><th>Approval</th><th>Payment</th><th>Amount</th></tr></thead><tbody>
        {loading?<tr><td colSpan="5">Loading…</td></tr>:requests.map(r=>{const p=paymentByRequest.get(r.id);const ps=r.status==='REJECTED'?'NOT_APPLICABLE':r.status!=='APPROVED'?'NOT_READY':p?.status==='PAID'?'PAID':'PAYMENT_PENDING';return <tr key={r.id}><td><b>#{r.id} · {r.title}</b></td><td>{r.requester?.fullName||r.requester?.username||'—'}<span className="subcell">{r.requester?.email||'—'}</span></td><td><Status s={r.status}/></td><td><Status s={ps}/></td><td>{money(r.totalAmount)}</td></tr>})}
        {!loading&&!requests.length&&<tr><td colSpan="5">No requests found.</td></tr>}
      </tbody></table></div>
    </Panel>
  </div>;
}

function ProductBoard(){
  const [products,setProducts]=useState([]);
  const [categories,setCategories]=useState([]);
  const [selected,setSelected]=useState('ALL');
  const [query,setQuery]=useState('');
  const [loading,setLoading]=useState(true);
  const [error,setError]=useState('');

  const load=async()=>{
    setLoading(true); setError('');
    try{
      const [productsRes,categoriesRes]=await Promise.all([api.get('/products'),api.get('/categories')]);
      setProducts(unwrap(productsRes)||[]);
      setCategories(unwrap(categoriesRes)||[]);
    }catch(e){setError(apiError(e))}
    finally{setLoading(false)}
  };
  useEffect(()=>{load()},[]);

  const available=useMemo(()=>products.filter(p=>p.status==='ACTIVE'),[products]);
  const filtered=useMemo(()=>available.filter(p=>{
    const categoryId=p.category?.id;
    const categoryMatch=selected==='ALL' || String(categoryId)===String(selected);
    const text=`${p.name||''} ${p.sku||''} ${p.description||''} ${p.supplier?.name||''}`.toLowerCase();
    return categoryMatch && text.includes(query.toLowerCase());
  }),[available,selected,query]);

  const categoryCount=(id)=>available.filter(p=>String(p.category?.id)===String(id)).length;

  return <div>
    <div className="page-intro product-board-intro">
      <div>
        <p className="eyebrow">PRODUCT CATALOG</p>
        <h1>Available products</h1>
        <p className="muted">Browse products currently available for procurement, grouped by category.</p>
      </div>
      <button className="secondary" onClick={load}><RefreshCw size={15}/> Refresh</button>
    </div>

    {error&&<div className="error"><AlertCircle size={16}/>{error}</div>}

    <div className="product-board">
      <aside className="category-panel">
        <div className="category-heading"><span>Categories</span><b>{available.length}</b></div>
        <button className={`category-btn ${selected==='ALL'?'active':''}`} onClick={()=>setSelected('ALL')}>
          <span>All products</span><b>{available.length}</b>
        </button>
        {categories.map(c=><button key={c.id} className={`category-btn ${String(selected)===String(c.id)?'active':''}`} onClick={()=>setSelected(c.id)}>
          <span>{c.name}</span><b>{categoryCount(c.id)}</b>
        </button>)}
        {!categories.length&&!loading&&<p className="category-empty">No categories found.</p>}
      </aside>

      <section className="product-results">
        <div className="product-toolbar">
          <div className="search"><Search size={16}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Search products, SKU or supplier..."/></div>
          <span className="result-count">{filtered.length} available</span>
        </div>

        {loading?<div className="panel product-loading"><RefreshCw className="spin" size={20}/><span>Loading product catalog...</span></div>:
        filtered.length?<div className="product-grid">{filtered.map(p=>{const stock=Number(p.availableStock??0);return <div className="product-card" key={p.id}>
          <div className="product-image-wrap">{(p.imageData||productImageFor(p))?<img src={p.imageData||productImageFor(p)} alt={p.name} className="product-image"/>:<div className="product-image-placeholder"><ImageIcon size={30}/><span>No image</span></div>}<span className={'status '+(stock>0?'green':'red')}>{stock>0?'AVAILABLE':'OUT OF STOCK'}</span></div>
          <span className="product-category">{p.category?.name||'Uncategorized'}</span>
          <h3>{p.name}</h3>
          <p>{p.description||'Available for procurement.'}</p>
          <div className="product-meta"><span>SKU <b>{p.sku}</b></span><strong>{money(p.price)}</strong></div>
          <div className="stock-line"><Boxes size={14}/><span>Available quantity</span><b className={stock>0?'stock-ok':'stock-zero'}>{stock}</b></div>
          <div className="product-supplier"><Building2 size={14}/><span>{p.supplier?.name||'Supplier not assigned'}{p.supplier?.upiId&&<small> · {p.supplier.upiId}</small>}</span></div>
        </div>})}</div>:
        <div className="panel empty-product"><Package size={28}/><h3>No available products</h3><p>There are no active products in this category or matching your search.</p></div>}
      </section>
    </div>
  </div>;
}

function UsersPage(){const [data,setData]=useState([]);useEffect(()=>{api.get('/users').then(r=>setData(unwrap(r)||[])).catch(()=>{})},[]);return <Panel title="Users"><div className="table-wrap"><table><thead><tr><th>User</th><th>Email</th><th>Department</th><th>Role</th><th>Status</th></tr></thead><tbody>{data.map(u=><tr key={u.id}><td><b>{u.fullName}</b><span className="subcell">{u.username}</span></td><td>{u.email}</td><td>{u.department?.name||'—'}</td><td><span className="tag">{u.role}</span></td><td><span className="status green">{u.status}</span></td></tr>)}</tbody></table></div></Panel>}
function Products(){const [data,setData]=useState([]);useEffect(()=>{api.get('/products').then(r=>setData(unwrap(r)||[])).catch(()=>{})},[]);return <Panel title="Product catalog"><div className="cards3">{data.map(p=><div className="mini-card" key={p.id}><div className="product-icon"><Package/></div><span>{p.sku}</span><h3>{p.name}</h3><b>{money(p.price)}</b><small>{p.supplier?.name||'—'}</small><small>{p.status}</small></div>)}{!data.length&&<p className="muted">No products found.</p>}</div></Panel>}
function Suppliers(){const [data,setData]=useState([]);useEffect(()=>{api.get('/suppliers').then(r=>setData(unwrap(r)||[])).catch(()=>{})},[]);const download=async id=>{try{const r=await api.get(`/payments/export/supplier/${id}`,{responseType:'blob'});const url=URL.createObjectURL(r.data);const a=document.createElement('a');a.href=url;a.download=`supplier-${id}-payments.csv`;a.click();URL.revokeObjectURL(url)}catch(e){alert(apiError(e))}};return <Panel title="Suppliers"><div className="cards3">{data.map(p=><div className="mini-card supplier-card" key={p.id}><div className="supplier-avatar">{p.name?.[0]}</div><h3>{p.name}</h3><small><Mail size={14}/> {p.email}</small><small>{p.phone||'—'}</small><small>UPI: {p.upiId||'Not configured'}</small><small>{p.address||'—'}</small><div className="supplier-foot"><span>ID #{p.id}</span><span className={'status '+(p.active?'green':'red')}>{p.active?'ACTIVE':'INACTIVE'}</span></div><button className="secondary" onClick={()=>download(p.id)}><Download size={14}/> Download payment history</button></div>)}</div></Panel>}
function Ratings(){const [data,setData]=useState([]);useEffect(()=>{api.get('/ratings').then(r=>setData(unwrap(r)||[])).catch(()=>{})},[]);const avg=data.length?(data.reduce((a,r)=>a+Number(r.ratingValue||0),0)/data.length).toFixed(1):'—';return <Panel title="Supplier ratings"><div className="rating-summary"><strong>{avg}</strong><div><div className="stars smallstars">★★★★★</div><span>{data.length} ratings</span></div></div><div className="table-wrap"><table><thead><tr><th>User</th><th>Supplier</th><th>Request</th><th>Rating</th><th>Comment</th></tr></thead><tbody>{data.map(r=><tr key={r.id}><td>{r.user?.fullName||r.user?.username||'—'}</td><td>{r.supplier?.name||'—'}</td><td>#{r.purchaseRequest?.id||'—'}</td><td><b>★ {r.ratingValue}/5</b></td><td>{r.comment||'—'}</td></tr>)}</tbody></table></div></Panel>}

function productImageFor(p){
  const text=`${p?.name||''} ${p?.description||''} ${p?.sku||''}`.toLowerCase();
  // Preloaded product photos supplied with this project.
  // These are used whenever the backend product does not have imageData.
  if(text.includes('laptop')||text.includes('notebook')||text.includes('dell latitude')||text.includes('computer')) return '/product-images/laptop.jpg';
  if(text.includes('printer')||text.includes('hp laser')||text.includes('printing')) return '/product-images/printer.jpg';
  if(text.includes('a4')||text.includes('sheet')||text.includes('paper')||text.includes('stationery')) return '/product-images/A4 sheets.jpg';
  if(text.includes('chair')||text.includes('office chair')||text.includes('visitor chair')) return '/product-images/chair.jpg';
  if(text.includes('table')||text.includes('desk')||text.includes('office table')) return '/product-images/table.jpg';
  return '';
}

function supplierIdOf(me){ return me?.supplierId || JSON.parse(localStorage.getItem(USER_KEY)||'{}')?.supplierId; }

function SupplierDashboard({me}){
  const sid=supplierIdOf(me);
  const [payments,setPayments]=useState([]); const [products,setProducts]=useState([]); const [tracking,setTracking]=useState([]);
  const [loading,setLoading]=useState(true); const [error,setError]=useState('');
  const load=async()=>{ if(!sid){setError('No supplier record is linked to this login. Ask the admin to link the supplier account.');setLoading(false);return;} setLoading(true); try{ const [p,pr,t]=await Promise.all([api.get(`/payments/supplier/${sid}/waiting-list`),api.get('/products'),api.get(`/payments/supplier/${sid}/tracking-status`)]); setPayments(unwrap(p)||[]); setProducts((unwrap(pr)||[]).filter(x=>String(x.supplier?.id)===String(sid))); setTracking((unwrap(t)||[]).filter(x=>x.shipmentStatus!=='DELIVERED')); }catch(e){setError(apiError(e))}finally{setLoading(false)} };
  useEffect(()=>{load()},[sid]);
  const pendingTracking=tracking.filter(x=>x.shipmentStatus!=='DELIVERED');
  return <div><div className="page-intro"><p className="eyebrow">SUPPLIER WORKSPACE</p><h1>Supplier Dashboard</h1><p className="muted">Manage your products, payment requests and shipment updates from one place.</p></div>{error&&<div className="error"><AlertCircle size={16}/>{error}</div>}<div className="stats-grid"><Stat icon={Package} label="My products" value={products.length} sub="Products assigned to you"/><Stat icon={Clock3} label="Pending delivery" value={pendingTracking.length} sub="Paid orders not yet delivered"/><Stat icon={CheckCircle2} label="Delivered" value="—" sub="Delivered orders are hidden from tracking"/><Stat icon={Truck} label="Supplier ID" value={sid||'—'} sub="Linked supplier record"/></div><div className="two-col"><Panel title="Recent payment requests"><div className="table-wrap"><table><thead><tr><th>Request</th><th>Amount</th><th>Status</th></tr></thead><tbody>{payments.slice(0,8).map(p=><tr key={p.id}><td><b>#{p.purchaseRequestId||'—'}</b><span className="subcell">{p.title||'Purchase request'}</span></td><td>{money(p.amount)}</td><td><Status s={p.paymentStatus}/></td></tr>)}</tbody></table></div>{!payments.length&&!loading&&<p className="muted">No payment requests yet.</p>}</Panel><Panel title="My products"><div className="cards3">{products.slice(0,6).map(p=><div className="mini-card" key={p.id}><div className="product-icon"><Package/></div><span>{p.sku}</span><h3>{p.name}</h3><b>{money(p.price)}</b><small>Stock: {p.availableStock??0}</small></div>)}</div>{!products.length&&!loading&&<p className="muted">No products assigned.</p>}</Panel></div><Panel title="Pending Tracking"><p className="muted" style={{marginTop:0}}>Only paid orders that are still pending delivery are shown here. Delivered orders are hidden.</p><div className="table-wrap"><table><thead><tr><th>Request</th><th>User</th><th>Product / Request</th><th>Shipment Status</th><th>Tracking No.</th><th>Current Location</th></tr></thead><tbody>{pendingTracking.map(t=><tr key={t.purchaseRequestId}><td><b>#{t.purchaseRequestId||'—'}</b></td><td><b>{t.userName||'—'}</b><span className="subcell">{t.userEmail||''}</span></td><td>{t.title||'—'}</td><td><Status s={t.shipmentStatus}/></td><td>{t.trackingNumber||'Not assigned'}</td><td>{t.currentLocation||'Awaiting supplier location update'}</td></tr>)}</tbody></table></div>{!pendingTracking.length&&!loading&&<p className="muted">No paid orders are pending delivery.</p>}</Panel></div>;
}

function SupplierProducts({me}){
  const sid=supplierIdOf(me);
  const [products,setProducts]=useState([]);
  const [categories,setCategories]=useState([]);
  const [showAddProduct,setShowAddProduct]=useState(false);
  const [saving,setSaving]=useState(false);
  const [error,setError]=useState('');
  const [message,setMessage]=useState('');
  const [newProduct,setNewProduct]=useState({name:'',description:'',price:'',sku:'',categoryId:'',availableStock:'1',imageData:''});

  const load=async()=>{
    if(!sid)return;
    try{
      const r=await api.get('/products');
      setProducts((unwrap(r)||[]).filter(p=>String(p.supplier?.id)===String(sid)));
    }catch(e){setError(apiError(e))}
  };
  useEffect(()=>{
    load();
    api.get('/categories').then(r=>setCategories(unwrap(r)||[])).catch(()=>{});
  },[sid]);

  const restock=async product=>{
    const current=Number(product.availableStock||0);
    const value=window.prompt(`Enter quantity to add for ${product.name}`, '10');
    if(value===null)return;
    const add=Number(value);
    if(!Number.isFinite(add)||add<=0){setError('Enter a valid quantity greater than 0.');return;}
    try{
      setError('');
      await api.put(`/products/${product.id}/stock`,{quantity:current+add});
      setMessage(`${add} units added to ${product.name}. Available stock: ${current+add}.`);
      await load();
    }catch(e){setError(apiError(e))}
  };

  const onImage=e=>{
    const file=e.target.files?.[0];
    if(!file)return;
    if(file.size>2*1024*1024){setError('Product image must be 2 MB or smaller.');return;}
    const reader=new FileReader();
    reader.onload=()=>setNewProduct(v=>({...v,imageData:reader.result}));
    reader.readAsDataURL(file);
  };

  const saveProduct=async e=>{
    e.preventDefault();
    if(!sid){setError('No supplier record is linked to this login.');return;}
    const stock=Math.max(0,Number(newProduct.availableStock||0));
    try{
      setSaving(true);setError('');setMessage('');
      const body={
        name:newProduct.name.trim(),description:newProduct.description.trim(),price:Number(newProduct.price),
        sku:newProduct.sku.trim(),availableStock:stock,imageData:newProduct.imageData||null,
        status:stock>0?'ACTIVE':'OUT_OF_STOCK',category:{id:Number(newProduct.categoryId)},supplier:{id:Number(sid)}
      };
      await api.post('/products',body);
      setMessage('Product added successfully to your supplier catalog.');
      setShowAddProduct(false);
      setNewProduct({name:'',description:'',price:'',sku:'',categoryId:'',availableStock:'1',imageData:''});
      await load();
    }catch(e){setError(apiError(e))}finally{setSaving(false)}
  };

  return <div>
    <div className="page-intro supplier-page-intro">
      <div><p className="eyebrow">PRODUCTS</p><h1>My Products</h1><p className="muted">Products assigned to your supplier account. If a product is out of stock, use <b>Add Stock</b> to replenish it.</p></div>
      <button className="primary" onClick={()=>setShowAddProduct(true)}><Plus size={16}/> Add Product</button>
    </div>
    {message&&<div className="success"><CheckCircle2 size={16}/>{message}</div>}
    {error&&<div className="error"><AlertCircle size={16}/>{error}</div>}
    <div className="product-grid">
      {products.map(p=>{
        const stock=Number(p.availableStock||0);
        return <div className="product-card" key={p.id}>
          <div className="product-image-wrap">{(p.imageData||productImageFor(p))?<img src={p.imageData||productImageFor(p)} alt={p.name} className="product-image"/>:<div className="product-image-placeholder"><ImageIcon size={30}/><span>No image</span></div>}<span className={'status '+(stock>0?'green':'red')}>{stock>0?'AVAILABLE':'OUT OF STOCK'}</span></div>
          <span className="product-category">{p.category?.name||'Uncategorized'}</span><h3>{p.name}</h3><p>{p.description||'—'}</p>
          <div className="product-meta"><span>SKU <b>{p.sku}</b></span><strong>{money(p.price)}</strong></div>
          <div className="stock-line"><Boxes size={14}/><span>Available quantity</span><b className={stock>0?'stock-ok':'stock-zero'}>{stock}</b></div>
          {stock<=0&&<div className="inventory-actions"><button className="primary wide" onClick={()=>restock(p)}><Plus size={14}/> Add Stock</button></div>}
        </div>
      })}
    </div>
    {!products.length&&<div className="panel"><p className="muted">No products found for this supplier. Click <b>Add Product</b> to add your first product.</p></div>}

    {showAddProduct&&<div className="modal-backdrop"><form className="payment-modal add-product-modal" onSubmit={saveProduct}>
      <div className="modal-head"><div><span className="small">SUPPLIER PRODUCT CATALOG</span><h2>Add Product</h2><p className="muted">The new product will automatically be assigned to your supplier account.</p></div><button type="button" className="icon-btn" onClick={()=>setShowAddProduct(false)}>×</button></div>
      <div className="form-grid"><Field label="Product name" placeholder="HP Laptop" value={newProduct.name} onChange={v=>setNewProduct({...newProduct,name:v})} required/><Field label="SKU" placeholder="HP-LAP-001" value={newProduct.sku} onChange={v=>setNewProduct({...newProduct,sku:v})} required/><Field label="Unit price" type="number" placeholder="50000" value={newProduct.price} onChange={v=>setNewProduct({...newProduct,price:v})} required/><Field label="Available quantity" type="number" placeholder="10" value={newProduct.availableStock} onChange={v=>setNewProduct({...newProduct,availableStock:v})} required/>
        <div><label className="label">Category</label><select className="input" value={newProduct.categoryId} onChange={e=>setNewProduct({...newProduct,categoryId:e.target.value})} required><option value="">Select category</option>{categories.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select></div>
        <div><label className="label">Product picture</label><input className="input" type="file" accept="image/png,image/jpeg,image/webp" onChange={onImage}/><p className="tiny">PNG/JPG/WebP · maximum 2 MB</p></div>
      </div>
      <label className="label">Description</label><textarea className="textarea" rows="4" value={newProduct.description} onChange={e=>setNewProduct({...newProduct,description:e.target.value})} placeholder="Describe the product..."/>
      {newProduct.imageData&&<div className="image-preview"><img src={newProduct.imageData} alt="Preview"/></div>}
      <div className="form-actions"><button type="button" className="secondary" onClick={()=>setShowAddProduct(false)}>Cancel</button><button className="primary" disabled={saving}>{saving?<RefreshCw className="spin" size={15}/>:<Plus size={15}/>} {saving?'Adding…':'Add Product'}</button></div>
    </form></div>}
  </div>;
}

function SupplierRatings({me}){
  const sid=supplierIdOf(me);
  const [ratings,setRatings]=useState([]);
  const [error,setError]=useState('');
  useEffect(()=>{
    if(!sid)return;
    api.get(`/ratings/supplier/${sid}`).then(r=>setRatings(unwrap(r)||[])).catch(e=>setError(apiError(e)));
  },[sid]);
  const avg=ratings.length?(ratings.reduce((sum,r)=>sum+Number(r.ratingValue||0),0)/ratings.length).toFixed(1):'—';
  const countForProduct=id=>ratings.filter(r=>String(r.product?.id)===String(id)).length;
  return <div>
    <div className="page-intro"><p className="eyebrow">CUSTOMER FEEDBACK</p><h1>User Ratings</h1><p className="muted">See ratings and comments submitted by users for your products.</p></div>
    {error&&<div className="error"><AlertCircle size={16}/>{error}</div>}
    <div className="stats-grid"><Stat icon={Star} label="Average rating" value={avg==='—'?'—':`${avg}/5`} sub="Across your products"/><Stat icon={Users} label="Total reviews" value={ratings.length} sub="Customer ratings"/><Stat icon={Package} label="Rated products" value={new Set(ratings.map(r=>r.product?.id).filter(Boolean)).size} sub="Products with feedback"/><Stat icon={Star} label="5-star reviews" value={ratings.filter(r=>Number(r.ratingValue)===5).length} sub="Highest ratings"/></div>
    <Panel title="Product ratings">
      <div className="table-wrap"><table><thead><tr><th>Product</th><th>User</th><th>Rating</th><th>Comment</th><th>Request</th></tr></thead><tbody>
      {ratings.map(r=><tr key={r.id}><td><b>{r.product?.name||'Product'}</b><span className="subcell">{r.product?.sku||''}</span></td><td>{r.user?.fullName||r.user?.username||'—'}</td><td><b>★ {r.ratingValue}/5</b><span className="subcell">{('★'.repeat(Number(r.ratingValue||0)) + '☆'.repeat(Math.max(0,5-Number(r.ratingValue||0))))}</span></td><td>{r.comment||'No comment'}</td><td>#{r.purchaseRequest?.id||'—'}</td></tr>)}
      </tbody></table></div>
      {!ratings.length&&!error&&<p className="muted">No user ratings have been submitted for your products yet.</p>}
    </Panel>
    <Panel title="Ratings by product">
      <div className="cards3">{[...new Map(ratings.filter(r=>r.product?.id).map(r=>[r.product.id,r.product])).values()].map(product=>{const rs=ratings.filter(r=>String(r.product?.id)===String(product.id));const a=(rs.reduce((x,r)=>x+Number(r.ratingValue||0),0)/rs.length).toFixed(1);return <div className="mini-card" key={product.id}><div className="product-icon"><Package/></div><span>{product.sku||'SKU'}</span><h3>{product.name}</h3><b>★ {a}/5</b><small>{rs.length} review{rs.length===1?'':'s'}</small></div>})}</div>
      {ratings.length===0&&<p className="muted">Product rating summaries will appear here after users rate their purchases.</p>}
    </Panel>
  </div>;
}

function SupplierPayments({me}){ const sid=supplierIdOf(me); const [data,setData]=useState([]); const [error,setError]=useState(''); useEffect(()=>{if(sid)api.get(`/payments/supplier/${sid}/waiting-list`).then(r=>setData(unwrap(r)||[])).catch(e=>setError(apiError(e)))},[sid]); return <div><div className="page-intro"><p className="eyebrow">PAYMENTS</p><h1>Supplier Payments</h1><p className="muted">See completed customer payments for your supplier account.</p></div>{error&&<div className="error"><AlertCircle size={16}/>{error}</div>}<Panel title="Paid orders"><div className="table-wrap"><table><thead><tr><th>Request</th><th>User</th><th>Amount</th><th>Payment</th><th>Shipment</th><th>Paid at</th></tr></thead><tbody>{data.map(p=><tr key={p.purchaseRequestId}><td>#{p.purchaseRequestId||'—'}<span className="subcell">{p.title||'—'}</span></td><td>{p.userName||'—'}<span className="subcell">{p.userEmail||''}</span></td><td>{money(p.amount)}</td><td><Status s={p.paymentStatus}/></td><td><Status s={p.shipmentStatus}/></td><td>{p.paidAt?.replace('T',' ')||'—'}</td></tr>)}</tbody></table></div>{!data.length&&<p className="muted">No paid orders waiting for shipment.</p>}</Panel></div> }

function SupplierShipments({me}){ const sid=supplierIdOf(me); const [requests,setRequests]=useState([]); const [requestId,setRequestId]=useState(''); const [status,setStatus]=useState(''); const [error,setError]=useState(''); const [statusPopup,setStatusPopup]=useState(''); const load=()=>{if(sid)api.get(`/payments/supplier/${sid}/waiting-list`).then(r=>setRequests(unwrap(r)||[])).catch(e=>setError(apiError(e)))}; useEffect(load,[sid]); const update=async()=>{if(!requestId||!status){setError('Select a request and shipment status.');return;}try{setError('');const endpointStatus={READY_TO_SHIP:'ready-to-ship',SHIPPED:'shipped',IN_TRANSIT:'in-transit',DELIVERED:'delivered'}[status]||String(status).toLowerCase().replaceAll('_','-'); await api.post(`/tracking/supplier/${requestId}/${endpointStatus}`,{supplierId:Number(sid),trackingNumber:'',currentLocation:'',note:''});setStatusPopup(status);setStatus('');load()}catch(e){setError(apiError(e))}}; const selectedRequest=requests.find(r=>String(r.purchaseRequestId)===String(requestId)); return <div><div className="page-intro"><p className="eyebrow">SHIPMENTS</p><h1>Shipment Updates</h1><p className="muted">Update the order status after payment and keep the customer informed.</p></div>{error&&<div className="error"><AlertCircle size={16}/>{error}</div>}<Panel title="Update shipment"><label className="label">Purchase request</label><select className="input" value={requestId} onChange={e=>setRequestId(e.target.value)}><option value="">Select request</option>{requests.map(r=><option key={r.purchaseRequestId||r.id} value={r.purchaseRequestId}>{`#${r.purchaseRequestId||'—'} · ${r.title||'Request'}`}</option>)}</select><label className="label">Status</label><select className="input" value={status} onChange={e=>setStatus(e.target.value)}><option value="">Select status</option><option value="READY_TO_SHIP">Ready to ship</option><option value="SHIPPED">Shipped</option><option value="IN_TRANSIT">In transit</option><option value="DELIVERED">Delivered</option></select><button className="primary" onClick={update}><Send size={16}/> Update status</button></Panel>{statusPopup&&<div className="status-popup-overlay" onClick={()=>setStatusPopup('')}><div className="status-popup" onClick={e=>e.stopPropagation()}><button className="payment-success-close" onClick={()=>setStatusPopup('')} aria-label="Close">×</button><div className="status-popup-icon"><CheckCircle2 size={30}/></div><h3>Shipment status updated</h3><p style={{marginBottom:8,fontWeight:700,color:'#243047'}}>#{requestId}{selectedRequest?.title?` · ${selectedRequest.title}`:''}</p><p>Status: <b>{String(statusPopup).replaceAll('_',' ')}</b></p><p style={{marginTop:8}}>The customer has been notified about the shipment update.</p><button className="primary" style={{marginTop:18}} onClick={()=>setStatusPopup('')}>Done</button></div></div>}</div> }

function SupplierReports({me}){ const sid=supplierIdOf(me); const download=async()=>{try{const r=await api.get(`/payments/export/supplier/${sid}`,{responseType:'blob'});const url=URL.createObjectURL(r.data);const a=document.createElement('a');a.href=url;a.download=`supplier-${sid}-payments.csv`;a.click();URL.revokeObjectURL(url)}catch(e){alert(apiError(e))}}; return <div><div className="page-intro"><p className="eyebrow">REPORTS</p><h1>Payment Reports</h1><p className="muted">Download your supplier payment history as a CSV file.</p></div><Panel title="Supplier payment history"><p className="muted">Supplier ID: #{sid||'—'}</p><button className="primary" disabled={!sid} onClick={download}><Download size={17}/> Download CSV</button></Panel></div> }

function SupplierConsole(){
  const [suppliers,setSuppliers]=useState([]);
  const [tracking,setTracking]=useState({purchaseRequestId:'',supplierId:''});
  const [waiting,setWaiting]=useState([]);
  const [products,setProducts]=useState([]);
  const [categories,setCategories]=useState([]);
  const [loadingWaiting,setLoadingWaiting]=useState(false);
  const [loadingProducts,setLoadingProducts]=useState(false);
  const [message,setMessage]=useState('');
  const [error,setError]=useState('');
  const [statusPopup,setStatusPopup]=useState('');
  const [selectedStatus,setSelectedStatus]=useState('');
  const [waitingQuery,setWaitingQuery]=useState('');
  const [showAddProduct,setShowAddProduct]=useState(false);
  const [savingProduct,setSavingProduct]=useState(false);
  const [newProduct,setNewProduct]=useState({name:'',description:'',price:'',sku:'',categoryId:'',availableStock:'0',imageData:''});

  useEffect(()=>{Promise.all([api.get('/suppliers'),api.get('/categories')]).then(([s,c])=>{setSuppliers(unwrap(s)||[]);setCategories(unwrap(c)||[])}).catch(e=>setError(apiError(e)))},[]);

  const loadWaiting=async supplierId=>{
    if(!supplierId){setWaiting([]);return;}
    setLoadingWaiting(true);setError('');
    try{const r=await api.get(`/payments/supplier/${supplierId}/waiting-list`);setWaiting(unwrap(r)||[])}catch(e){setError(apiError(e));setWaiting([])}finally{setLoadingWaiting(false)}
  };
  const loadProducts=async supplierId=>{
    if(!supplierId){setProducts([]);return;}
    setLoadingProducts(true);
    try{const r=await api.get('/products');setProducts((unwrap(r)||[]).filter(p=>String(p.supplier?.id)===String(supplierId)))}catch(e){setError(apiError(e));setProducts([])}finally{setLoadingProducts(false)}
  };
  const chooseSupplier=e=>{
    const supplierId=e.target.value;
    setTracking({...tracking,supplierId}); setWaitingQuery(''); setSelectedStatus('');
    loadWaiting(supplierId); loadProducts(supplierId);
    setNewProduct({...newProduct,categoryId:'',availableStock:'0'});
  };
  useEffect(()=>{
    if(!tracking.supplierId) return;
    const timer=setInterval(()=>{loadWaiting(tracking.supplierId);loadProducts(tracking.supplierId)},5000);
    return ()=>clearInterval(timer);
  },[tracking.supplierId]);
  const update=async status=>{
    try{
      if(!tracking.supplierId||!tracking.purchaseRequestId){setError('Select a supplier and enter a purchase request ID.');return;}
      setSelectedStatus(status);setError('');
      const body={supplierId:Number(tracking.supplierId),trackingNumber:'',currentLocation:'',note:''};
      await api.post(`/tracking/supplier/${tracking.purchaseRequestId}/${status}`,body);
      const label=status.replaceAll('-',' ').toUpperCase();setStatusPopup(label);setMessage(`Shipment status updated to ${label}.`);
      await loadWaiting(tracking.supplierId);setTimeout(()=>setStatusPopup(''),2200);
    }catch(e){setError(apiError(e))}
  };
  const downloadCsv=async()=>{if(!tracking.supplierId){setError('Select a supplier first.');return;}try{const r=await api.get(`/payments/export/supplier/${tracking.supplierId}`,{responseType:'blob'});const url=URL.createObjectURL(r.data);const a=document.createElement('a');a.href=url;a.download=`supplier-${tracking.supplierId}-products-payment-history.csv`;a.click();URL.revokeObjectURL(url)}catch(e){setError(apiError(e))}};
  const onImage=event=>{const file=event.target.files?.[0];if(!file)return;if(file.size>2*1024*1024){setError('Product image must be 2 MB or smaller.');return;}const reader=new FileReader();reader.onload=()=>setNewProduct(v=>({...v,imageData:reader.result}));reader.readAsDataURL(file)};
  const saveProduct=async e=>{
    e.preventDefault();
    if(!tracking.supplierId){setError('Select a supplier first.');return;}
    setSavingProduct(true);setError('');setMessage('');
    try{
      const stock=Math.max(0,Number(newProduct.availableStock||0));
      const body={name:newProduct.name.trim(),description:newProduct.description.trim(),price:Number(newProduct.price),sku:newProduct.sku.trim(),availableStock:stock,imageData:newProduct.imageData||null,status:stock>0?'ACTIVE':'OUT_OF_STOCK',category:{id:Number(newProduct.categoryId)},supplier:{id:Number(tracking.supplierId)}};
      await api.post('/products',body);
      setMessage('Product added successfully.');setShowAddProduct(false);setNewProduct({name:'',description:'',price:'',sku:'',categoryId:'',availableStock:'0',imageData:''});await loadProducts(tracking.supplierId);
    }catch(e){setError(apiError(e))}finally{setSavingProduct(false)}
  };
  const restock=async(product,quantity)=>{try{const qty=Number(quantity);if(!Number.isFinite(qty)||qty<0){setError('Enter a valid stock quantity.');return;}await api.put(`/products/${product.id}/stock`,{quantity:qty});setMessage(`${product.name} stock updated to ${qty}.`);await loadProducts(tracking.supplierId)}catch(e){setError(apiError(e))}};
  const statusText=s=>String(s||'NOT_SHIPPED').replaceAll('_',' ');
  const statusClass=s=>s==='DELIVERED'?'green':s==='IN_TRANSIT'?'blue':s==='SHIPPED'?'orange':'blue';

  return <div>
    <div className="page-intro supplier-page-intro"><div><p className="eyebrow">SUPPLIER CONSOLE</p><h1>Supplier inventory & updates</h1><p className="muted">Check available quantities, add products, update shipment status and monitor paid orders waiting for shipment.</p></div>{tracking.supplierId&&<button className="primary" onClick={()=>setShowAddProduct(true)}><Plus size={16}/> Add Product</button>}</div>
    {message&&<div className="success"><CheckCircle2 size={16}/>{message}</div>}{error&&<div className="error"><AlertCircle size={16}/>{error}</div>}

    <div className="supplier-console-center"><Panel title="Supplier inventory"><div className="supplier-status-form"><div><label className="label">Supplier</label><select className="input" value={tracking.supplierId} onChange={chooseSupplier}><option value="">Select supplier</option>{suppliers.map(s=><option key={s.id} value={s.id}>{s.name} (#{s.id})</option>)}</select></div><div className="inventory-summary"><span>Total products</span><b>{products.length}</b><small>{products.filter(p=>Number(p.availableStock??0)>0).length} in stock · {products.filter(p=>Number(p.availableStock??0)<=0).length} out of stock</small></div></div></Panel></div>

    <div className="panel supplier-inventory-panel"><div className="panel-head"><div><h3>Product availability</h3><p className="muted">See exactly how many units the selected supplier currently has.</p></div>{tracking.supplierId&&<button className="secondary" onClick={()=>setShowAddProduct(true)}><Plus size={14}/> Add Product</button>}</div>
      {!tracking.supplierId?<p className="muted">Select a supplier to view product quantities.</p>:loadingProducts?<p className="muted">Loading product inventory…</p>:<div className="inventory-grid">{products.map(p=>{const stock=Number(p.availableStock??0);return <div className="inventory-card" key={p.id}><div className="inventory-image">{p.imageData?<img src={p.imageData} alt={p.name}/>:<ImageIcon size={28}/>}</div><div className="inventory-info"><div className="inventory-title"><div><span className="small">SKU {p.sku}</span><h3>{p.name}</h3></div><span className={'status '+(stock>0?'green':'red')}>{stock>0?'IN STOCK':'OUT OF STOCK'}</span></div><p>{p.description||'No description'}</p><div className="inventory-qty"><span>Available quantity</span><strong className={stock>0?'stock-ok':'stock-zero'}>{stock}</strong><span>units</span></div><div className="inventory-actions">{stock<=0&&<button className="primary" onClick={()=>setShowAddProduct(true)}><Plus size={14}/> Add Product</button>}<button className="secondary" onClick={()=>{const q=window.prompt(`Enter new quantity for ${p.name}`,String(stock));if(q!==null)restock(p,q)}}><Pencil size={14}/> Update quantity</button></div></div></div>})}{!products.length&&<div className="empty-product"><Package size={28}/><h3>No products assigned</h3><p>Use Add Product to add the first product for this supplier.</p></div>}</div>}
    </div>

    <div className="supplier-console-center"><Panel title="Shipment status"><div className="supplier-status-form"><Field label="Purchase request ID" placeholder="101" value={tracking.purchaseRequestId} onChange={v=>setTracking({...tracking,purchaseRequestId:v})} required/><div><label className="label">Supplier</label><select className="input" value={tracking.supplierId} onChange={chooseSupplier}><option value="">Select supplier</option>{suppliers.map(s=><option key={s.id} value={s.id}>{s.name} (#{s.id})</option>)}</select></div></div><div className="shipment-actions supplier-status-actions"><button className={selectedStatus==='ready-to-ship'?'status-selected':'secondary'} onClick={()=>update('ready-to-ship')}>READY TO SHIP</button><button className={selectedStatus==='shipped'?'status-selected':'secondary'} onClick={()=>update('shipped')}>SHIPPED</button><button className={selectedStatus==='in-transit'?'status-selected':'secondary'} onClick={()=>update('in-transit')}>IN TRANSIT</button><button className={selectedStatus==='delivered'?'status-selected':'secondary'} onClick={()=>update('delivered')}>DELIVERED</button></div></Panel></div>

    <div className="panel supplier-waiting-panel"><div className="panel-head"><div><h3>Users waiting for shipment</h3><p className="muted">Paid requests for the selected supplier that are waiting to be shipped or delivered.</p></div>{tracking.supplierId&&<div className="supplier-panel-actions"><span className="status blue">{waiting.length} WAITING</span><button className="secondary" onClick={downloadCsv}><Download size={14}/> Download my products & payment history CSV</button></div>}</div>{!tracking.supplierId?<p className="muted">Select a supplier above to view paid users waiting for shipment.</p>:<><div className="filters supplier-waiting-filter"><div className="search"><Search size={16}/><input value={waitingQuery} onChange={e=>setWaitingQuery(e.target.value)} placeholder="Search user, request or product..."/></div></div><div className="table-wrap"><table><thead><tr><th>Request ID</th><th>User</th><th>Product Request</th><th>Payment</th><th>Shipment Status</th><th>Paid At</th></tr></thead><tbody>{loadingWaiting?<tr><td colSpan="6">Loading waiting list…</td></tr>:waiting.filter(row=>`${row.purchaseRequestId||''} ${row.userName||''} ${row.userEmail||''} ${row.title||''}`.toLowerCase().includes(waitingQuery.toLowerCase())).length?waiting.filter(row=>`${row.purchaseRequestId||''} ${row.userName||''} ${row.userEmail||''} ${row.title||''}`.toLowerCase().includes(waitingQuery.toLowerCase())).map(row=><tr key={row.purchaseRequestId}><td><b>#{row.purchaseRequestId}</b></td><td><b>{row.userName||'—'}</b><span className="subcell">{row.userEmail||'—'}</span></td><td>{row.title||'Purchase request'}<span className="subcell">{money(row.amount)}</span></td><td><Status s={row.paymentStatus}/></td><td><span className={'status '+statusClass(row.shipmentStatus)}>{statusText(row.shipmentStatus)}</span></td><td>{row.paidAt?String(row.paidAt).replace('T',' '):'Pending'}</td></tr>):<tr><td colSpan="6">No matching users are waiting for this supplier.</td></tr>}</tbody></table></div></>}</div>

    {showAddProduct&&<div className="modal-backdrop"><form className="payment-modal add-product-modal" onSubmit={saveProduct}><div className="modal-head"><div><span className="small">SUPPLIER PRODUCT CATALOG</span><h2>Add Product</h2><p className="muted">Add a product, quantity and picture to the selected supplier.</p></div><button type="button" className="icon-btn" onClick={()=>setShowAddProduct(false)}>×</button></div><div className="form-grid"><Field label="Product name" placeholder="HP Laptop" value={newProduct.name} onChange={v=>setNewProduct({...newProduct,name:v})} required/><Field label="SKU" placeholder="HP-LAP-001" value={newProduct.sku} onChange={v=>setNewProduct({...newProduct,sku:v})} required/><Field label="Unit price" type="number" placeholder="50000" value={newProduct.price} onChange={v=>setNewProduct({...newProduct,price:v})} required/><Field label="Available quantity" type="number" placeholder="10" value={newProduct.availableStock} onChange={v=>setNewProduct({...newProduct,availableStock:v})} required/><div><label className="label">Category</label><select className="input" value={newProduct.categoryId} onChange={e=>setNewProduct({...newProduct,categoryId:e.target.value})} required><option value="">Select category</option>{categories.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select></div><div><label className="label">Product picture</label><input className="input" type="file" accept="image/png,image/jpeg,image/webp" onChange={onImage}/><p className="tiny">PNG/JPG/WebP · maximum 2 MB</p></div></div><label className="label">Description</label><textarea className="textarea" rows="4" value={newProduct.description} onChange={e=>setNewProduct({...newProduct,description:e.target.value})} placeholder="Describe the product..."/>{newProduct.imageData&&<div className="image-preview"><img src={newProduct.imageData} alt="Preview"/></div>}<div className="form-actions"><button type="button" className="secondary" onClick={()=>setShowAddProduct(false)}>Cancel</button><button className="primary" disabled={savingProduct}>{savingProduct?<RefreshCw className="spin" size={15}/>:<Plus size={15}/>} {savingProduct?'Adding…':'Add Product'}</button></div></form></div>}

    {statusPopup&&<div className="status-popup-overlay"><div className="status-popup"><div className="status-popup-icon"><CheckCircle2 size={30}/></div><h3>{statusPopup}</h3><p>Shipment status updated successfully.</p></div></div>}
  </div>;
}

createRoot(document.getElementById('root')).render(<BrowserRouter><App/></BrowserRouter>);
