import AssessmentOutlinedIcon from '@mui/icons-material/AssessmentOutlined';
import AutoAwesomeOutlinedIcon from '@mui/icons-material/AutoAwesomeOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import CampaignOutlinedIcon from '@mui/icons-material/CampaignOutlined';
import ChevronLeftRoundedIcon from '@mui/icons-material/ChevronLeftRounded';
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded';
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import EventOutlinedIcon from '@mui/icons-material/EventOutlined';
import ExpandMoreRoundedIcon from '@mui/icons-material/ExpandMoreRounded';
import HistoryRoundedIcon from '@mui/icons-material/HistoryRounded';
import LogoutRoundedIcon from '@mui/icons-material/LogoutRounded';
import MenuRoundedIcon from '@mui/icons-material/MenuRounded';
import NotificationsNoneRoundedIcon from '@mui/icons-material/NotificationsNoneRounded';
import PeopleAltOutlinedIcon from '@mui/icons-material/PeopleAltOutlined';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import SearchRoundedIcon from '@mui/icons-material/SearchRounded';
import {
  AppBar,
  Avatar,
  Badge,
  Box,
  ButtonBase,
  Divider,
  Drawer,
  IconButton,
  InputAdornment,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  TextField,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useMemo, useState, type MouseEvent, type ReactNode } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/authState';
import { getAdminDashboard } from '../api/adminDashboardApi';
import { BrandMark } from '../components/BrandMark';

const drawerWidth = 264;
const collapsedDrawerWidth = 80;

const navigation = [
  { section: 'TỔNG QUAN', items: [
    { label: 'Tổng quan', path: '/', icon: <DashboardOutlinedIcon /> },
  ] },
  { section: 'QUẢN LÝ', items: [
    { label: 'Học sinh', path: '/students', icon: <PeopleAltOutlinedIcon /> },
    { label: 'Học vụ', path: '/academics', icon: <SchoolOutlinedIcon /> },
    { label: 'Lịch học', path: '/timetable', icon: <CalendarMonthOutlinedIcon /> },
    { label: 'Điểm số', path: '/grades', icon: <AssessmentOutlinedIcon /> },
    { label: 'Đơn từ', path: '/forms', icon: <DescriptionOutlinedIcon /> },
    { label: 'Thông báo', path: '/notifications', icon: <CampaignOutlinedIcon /> },
    { label: 'Sự kiện & CLB', path: '/activities', icon: <EventOutlinedIcon /> },
  ] },
  { section: 'HỆ THỐNG', items: [
    { label: 'Cấu hình AI', path: '/ai-settings', icon: <AutoAwesomeOutlinedIcon /> },
    { label: 'Nhật ký', path: '/audit', icon: <HistoryRoundedIcon /> },
  ] },
];

const pageTitles = Object.fromEntries(
  navigation.flatMap((group) => group.items.map((item) => [item.path, item.label])),
);

export function AdminShell({ children }: { children: ReactNode }) {
  const { account, accessToken, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const [accountAnchor, setAccountAnchor] = useState<HTMLElement | null>(null);
  const [notificationAnchor, setNotificationAnchor] = useState<HTMLElement | null>(null);
  const [search, setSearch] = useState('');
  const activeWidth = collapsed ? collapsedDrawerWidth : drawerWidth;
  const title = useMemo(() => pageTitles[location.pathname] ?? 'FSchool Admin', [location.pathname]);
  const dashboardQuery = useQuery({
    queryKey: ['admin-dashboard'],
    queryFn: () => getAdminDashboard(accessToken!),
    enabled: Boolean(accessToken),
    staleTime: 60_000,
  });
  const operationalNotices = useMemo(() => {
    const metrics = dashboardQuery.data?.metrics;
    if (!metrics) return [];
    return [
      { count: metrics.pendingForms, label: 'đơn từ đang chờ xử lý', path: '/forms' },
      { count: metrics.pendingClubApplications, label: 'đơn CLB đang chờ duyệt', path: '/activities' },
      { count: metrics.upcomingEvents, label: 'sự kiện sắp diễn ra', path: '/activities' },
      { count: metrics.recentlyUpdatedGrades, label: 'điểm mới được cập nhật', path: '/grades' },
    ].filter((notice) => notice.count > 0);
  }, [dashboardQuery.data]);
  const operationalNoticeCount = operationalNotices.reduce((total, notice) => total + notice.count, 0);

  const handleLogout = async () => {
    setAccountAnchor(null);
    await logout();
    navigate('/login', { replace: true });
  };

  const handleSearch = () => {
    const value = search.trim();
    if (value) {
      navigate(`/students?search=${encodeURIComponent(value)}`);
    }
  };

  const drawer = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', overflowX: 'hidden' }}>
      <Toolbar sx={{ px: collapsed ? 2.6 : 2.5, minHeight: '72px !important' }}>
        <BrandMark compact={collapsed} />
      </Toolbar>
      <Divider />
      <List sx={{ px: 1.5, py: 1.5 }}>
        {navigation.map((group) => (
          <Box key={group.section} sx={{ mb: 1.5 }}>
            {!collapsed && (
              <Typography sx={{ px: 1.5, py: 1, fontSize: 10.5, fontWeight: 700, color: 'text.secondary', letterSpacing: '0.08em' }}>
                {group.section}
              </Typography>
            )}
            {group.items.map((item) => {
              const selected = location.pathname === item.path;
              const button = (
                <ListItemButton
                  key={item.path}
                  selected={selected}
                  onClick={() => { navigate(item.path); setMobileOpen(false); }}
                  sx={{
                    minHeight: 42,
                    px: collapsed ? 2 : 1.5,
                    mb: 0.4,
                    borderRadius: 2,
                    justifyContent: collapsed ? 'center' : 'flex-start',
                    '&.Mui-selected': { bgcolor: '#fff0eb', color: 'primary.main' },
                    '&.Mui-selected:hover': { bgcolor: '#ffe8df' },
                  }}
                >
                  <ListItemIcon sx={{ minWidth: collapsed ? 0 : 40, color: 'inherit', justifyContent: 'center', '& svg': { fontSize: 20 } }}>
                    {item.icon}
                  </ListItemIcon>
                  {!collapsed && <ListItemText primary={item.label} slotProps={{ primary: { sx: { fontSize: 13.5, fontWeight: selected ? 650 : 500 } } }} />}
                </ListItemButton>
              );
              return collapsed ? <Tooltip key={item.path} title={item.label} placement="right">{button}</Tooltip> : button;
            })}
          </Box>
        ))}
      </List>
      <Box sx={{ mt: 'auto', p: 1.5, borderTop: 1, borderColor: 'divider' }}>
        <Tooltip title={collapsed ? 'Mở rộng menu' : 'Thu gọn menu'} placement="right">
          <ListItemButton aria-label={collapsed ? 'Mở rộng menu' : 'Thu gọn menu'} onClick={() => setCollapsed((value) => !value)} sx={{ minHeight: 40, borderRadius: 2, justifyContent: collapsed ? 'center' : 'flex-start' }}>
            <ListItemIcon sx={{ minWidth: collapsed ? 0 : 40, justifyContent: 'center' }}>
              {collapsed ? <ChevronRightRoundedIcon /> : <ChevronLeftRoundedIcon />}
            </ListItemIcon>
            {!collapsed && <ListItemText primary="Thu gọn menu" slotProps={{ primary: { sx: { fontSize: 13 } } }} />}
          </ListItemButton>
        </Tooltip>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', bgcolor: 'background.default' }}>
      <AppBar
        position="fixed"
        color="inherit"
        elevation={0}
        sx={{
          borderBottom: 1,
          borderColor: 'divider',
          width: { md: `calc(100% - ${activeWidth}px)` },
          ml: { md: `${activeWidth}px` },
          transition: 'width 180ms ease, margin 180ms ease',
        }}
      >
        <Toolbar sx={{ minHeight: '72px !important', px: { xs: 2, sm: 3 } }}>
          <IconButton aria-label="Mở menu" onClick={() => setMobileOpen(true)} sx={{ display: { md: 'none' }, mr: 1 }}>
            <MenuRoundedIcon />
          </IconButton>
          <Box>
            <Typography sx={{ fontSize: 10.5, color: 'text.secondary', fontWeight: 650, letterSpacing: '0.06em' }}>FSCHOOL ADMIN</Typography>
            <Typography component="p" sx={{ fontSize: 17, fontWeight: 680 }}>{title}</Typography>
          </Box>
          <Box sx={{ flexGrow: 1 }} />
          <TextField
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            onKeyDown={(event) => { if (event.key === 'Enter') handleSearch(); }}
            placeholder="Tìm học sinh…"
            aria-label="Tìm học sinh"
            sx={{ display: { xs: 'none', lg: 'block' }, width: 260, mr: 1.5, '& .MuiOutlinedInput-root': { bgcolor: 'background.default' } }}
            slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchRoundedIcon fontSize="small" /></InputAdornment> } }}
          />
          <IconButton aria-label="Mở thông báo" onClick={(event) => setNotificationAnchor(event.currentTarget)} sx={{ mr: 1 }}>
            <Badge badgeContent={operationalNoticeCount} max={99} color="primary"><NotificationsNoneRoundedIcon /></Badge>
          </IconButton>
          <ButtonBase
            aria-label="Mở menu tài khoản"
            onClick={(event: MouseEvent<HTMLElement>) => setAccountAnchor(event.currentTarget)}
            sx={{ borderRadius: 2, p: 0.6, pl: 0.8, gap: 1 }}
          >
            <Avatar sx={{ width: 34, height: 34, bgcolor: 'primary.main', fontSize: 13, fontWeight: 700 }}>
              {account?.fullName.charAt(0).toUpperCase()}
            </Avatar>
            <Box sx={{ display: { xs: 'none', sm: 'block' }, textAlign: 'left', minWidth: 112 }}>
              <Typography variant="body2" sx={{ fontWeight: 650 }}>{account?.fullName}</Typography>
              <Typography variant="caption" color="text.secondary">Quản trị viên</Typography>
            </Box>
            <ExpandMoreRoundedIcon sx={{ display: { xs: 'none', sm: 'block' }, color: 'text.secondary', fontSize: 18 }} />
          </ButtonBase>
        </Toolbar>
      </AppBar>

      <Menu anchorEl={notificationAnchor} open={Boolean(notificationAnchor)} onClose={() => setNotificationAnchor(null)} anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }} transformOrigin={{ horizontal: 'right', vertical: 'top' }} slotProps={{ paper: { sx: { width: 340, mt: 1 } } }}>
        <Box sx={{ px: 2, py: 1 }}><Typography variant="body2" sx={{ fontWeight: 700 }}>Thông báo mới</Typography></Box>
        <Divider />
        {operationalNotices.length === 0 && (
          <MenuItem disabled sx={{ whiteSpace: 'normal', py: 1.25, fontSize: 13 }}>Không có công việc mới cần chú ý.</MenuItem>
        )}
        {operationalNotices.map((notice) => (
          <MenuItem
            key={notice.path + notice.label}
            onClick={() => { setNotificationAnchor(null); navigate(notice.path); }}
            sx={{ whiteSpace: 'normal', py: 1.25, fontSize: 13 }}
          >
            {notice.count} {notice.label}
          </MenuItem>
        ))}
      </Menu>
      <Menu anchorEl={accountAnchor} open={Boolean(accountAnchor)} onClose={() => setAccountAnchor(null)} transformOrigin={{ horizontal: 'right', vertical: 'top' }} anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}>
        <MenuItem onClick={handleLogout}><ListItemIcon><LogoutRoundedIcon fontSize="small" /></ListItemIcon>Đăng xuất</MenuItem>
      </Menu>

      <Box component="nav" aria-label="Điều hướng quản trị" sx={{ width: { md: activeWidth }, flexShrink: { md: 0 }, transition: 'width 180ms ease' }}>
        <Drawer variant="temporary" open={mobileOpen} onClose={() => setMobileOpen(false)} ModalProps={{ keepMounted: true }} sx={{ display: { xs: 'block', md: 'none' }, '& .MuiDrawer-paper': { width: drawerWidth } }}>
          {drawer}
        </Drawer>
        <Drawer variant="permanent" open sx={{ display: { xs: 'none', md: 'block' }, '& .MuiDrawer-paper': { width: activeWidth, transition: 'width 180ms ease', overflowX: 'hidden' } }}>
          {drawer}
        </Drawer>
      </Box>

      <Box component="main" sx={{ flexGrow: 1, minWidth: 0, px: { xs: 2, sm: 3, xl: 4 }, pb: 4, pt: { xs: 11, sm: 12 } }}>
        {children}
      </Box>
    </Box>
  );
}
