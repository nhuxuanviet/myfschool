import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded';
import AssessmentOutlinedIcon from '@mui/icons-material/AssessmentOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import EventOutlinedIcon from '@mui/icons-material/EventOutlined';
import FiberManualRecordRoundedIcon from '@mui/icons-material/FiberManualRecordRounded';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';
import PeopleAltOutlinedIcon from '@mui/icons-material/PeopleAltOutlined';
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Divider,
  Grid,
  LinearProgress,
  Skeleton,
  Stack,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { getAdminDashboard, type AdminDashboardMetrics } from '../api/adminDashboardApi';
import { useAuth } from '../auth/authState';

const metricDefinitions: Array<{
  key: keyof AdminDashboardMetrics;
  label: string;
  helper: string;
  icon: React.ReactNode;
  color: string;
  background: string;
}> = [
  { key: 'totalStudents', label: 'Học sinh', helper: 'Tài khoản đang hoạt động', icon: <PeopleAltOutlinedIcon />, color: '#2563eb', background: '#eff6ff' },
  { key: 'activeClasses', label: 'Lớp học', helper: 'Lớp có học sinh', icon: <GroupsOutlinedIcon />, color: '#7c3aed', background: '#f5f3ff' },
  { key: 'pendingForms', label: 'Đơn chờ xử lý', helper: 'Cần phản hồi', icon: <DescriptionOutlinedIcon />, color: '#d97706', background: '#fff7ed' },
  { key: 'upcomingEvents', label: 'Sự kiện sắp tới', helper: 'Từ hôm nay', icon: <EventOutlinedIcon />, color: '#db2777', background: '#fdf2f8' },
  { key: 'pendingClubApplications', label: 'Đăng ký CLB', helper: 'Đang chờ duyệt', icon: <CalendarMonthOutlinedIcon />, color: '#059669', background: '#ecfdf5' },
  { key: 'recentlyUpdatedGrades', label: 'Điểm vừa cập nhật', helper: 'Trong 7 ngày qua', icon: <AssessmentOutlinedIcon />, color: '#f4511e', background: '#fff1eb' },
];

const activityLabels: Record<string, string> = {
  ADMIN_LOGIN_SUCCEEDED: 'Đăng nhập vào hệ thống quản trị',
  ADMIN_LOGIN_FAILED: 'Có một lần đăng nhập không thành công',
  ADMIN_LOGIN_RATE_LIMITED: 'Tài khoản đăng nhập bị giới hạn tạm thời',
  ADMIN_REFRESH_SUCCEEDED: 'Phiên đăng nhập được gia hạn',
  ADMIN_REFRESH_FAILED: 'Gia hạn phiên đăng nhập không thành công',
  ADMIN_LOGOUT: 'Đăng xuất khỏi hệ thống quản trị',
};

function relativeTime(value: string): string {
  const minutes = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 60_000));
  if (minutes < 1) return 'Vừa xong';
  if (minutes < 60) return `${minutes} phút trước`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} giờ trước`;
  return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(value));
}

function greeting(): string {
  const hour = Number(new Intl.DateTimeFormat('en-US', {
    hour: '2-digit',
    hour12: false,
    timeZone: 'Asia/Ho_Chi_Minh',
  }).format(new Date()));
  if (hour < 11) return 'Chào buổi sáng';
  if (hour < 18) return 'Chào buổi chiều';
  return 'Chào buổi tối';
}

export function DashboardPage() {
  const { account, accessToken } = useAuth();
  const navigate = useNavigate();
  const dashboard = useQuery({
    queryKey: ['admin-dashboard'],
    queryFn: () => getAdminDashboard(accessToken!),
    enabled: Boolean(accessToken),
  });

  if (dashboard.isError) {
    return (
      <Box sx={{ maxWidth: 1280, mx: 'auto' }}>
        <Typography component="h1" variant="h1">Tổng quan</Typography>
        <Alert severity="error" sx={{ mt: 3 }} action={<Button color="inherit" startIcon={<RefreshRoundedIcon />} onClick={() => dashboard.refetch()}>Thử lại</Button>}>
          Không thể tải dữ liệu tổng quan. Vui lòng thử lại.
        </Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ maxWidth: 1280, mx: 'auto' }}>
      <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ justifyContent: 'space-between', alignItems: { sm: 'center' }, gap: 2, mb: 2.5 }}>
        <Box>
          <Typography component="h1" variant="h1">{greeting()}, {account?.fullName}</Typography>
          <Typography color="text.secondary" sx={{ mt: 0.5 }}>Đây là tình hình vận hành mới nhất của nhà trường.</Typography>
        </Box>
        <Chip label="Dữ liệu thời gian thực" size="small" color="success" variant="outlined" />
      </Stack>

      <Grid container spacing={2}>
        {metricDefinitions.map((metric) => (
          <Grid key={metric.key} size={{ xs: 12, sm: 6, lg: 4 }}>
            <Card sx={{ height: 126 }}>
              <CardContent sx={{ p: 2.25, '&:last-child': { pb: 2.25 } }}>
                <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <Box>
                    <Typography color="text.secondary" sx={{ fontSize: 12.5, fontWeight: 600 }}>{metric.label}</Typography>
                    {dashboard.data
                      ? <Typography sx={{ mt: 0.5, fontSize: 27, lineHeight: 1.2, fontWeight: 720 }}>{dashboard.data.metrics[metric.key].toLocaleString('vi-VN')}</Typography>
                      : <Skeleton width={58} height={38} />}
                  </Box>
                  <Box sx={{ width: 38, height: 38, borderRadius: 2, display: 'grid', placeItems: 'center', color: metric.color, bgcolor: metric.background, '& svg': { fontSize: 20 } }}>{metric.icon}</Box>
                </Stack>
                <Typography color="text.secondary" sx={{ mt: 1.25, fontSize: 11.5 }}>{metric.helper}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={2} sx={{ mt: 0 }}>
        <Grid size={{ xs: 12, lg: 7 }}>
          <Card sx={{ height: '100%' }}>
            <CardContent sx={{ p: 0 }}>
              <Box sx={{ px: 2.5, py: 2.1 }}>
                <Typography component="h2" variant="h2">Việc cần xử lý</Typography>
                <Typography color="text.secondary" sx={{ mt: 0.35, fontSize: 12.5 }}>Ưu tiên các yêu cầu đang chờ phản hồi.</Typography>
              </Box>
              <Divider />
              {dashboard.data ? (
                <Stack divider={<Divider flexItem />}>
                  {[
                    { label: 'Đơn từ học sinh', value: dashboard.data.metrics.pendingForms, path: '/forms', color: '#d97706' },
                    { label: 'Đăng ký câu lạc bộ', value: dashboard.data.metrics.pendingClubApplications, path: '/activities', color: '#059669' },
                    { label: 'Sự kiện sắp diễn ra', value: dashboard.data.metrics.upcomingEvents, path: '/activities', color: '#db2777' },
                  ].map((item) => (
                    <Button key={item.label} color="inherit" onClick={() => navigate(item.path)} sx={{ px: 2.5, py: 1.65, justifyContent: 'flex-start', borderRadius: 0, textAlign: 'left' }}>
                      <FiberManualRecordRoundedIcon sx={{ fontSize: 10, color: item.color, mr: 1.5 }} />
                      <Box sx={{ flexGrow: 1 }}><Typography variant="body2" sx={{ fontWeight: 620 }}>{item.label}</Typography></Box>
                      <Chip label={item.value} size="small" sx={{ mr: 1.5, minWidth: 36 }} />
                      <ArrowForwardRoundedIcon sx={{ fontSize: 18, color: 'text.secondary' }} />
                    </Button>
                  ))}
                </Stack>
              ) : <Box sx={{ px: 2.5, py: 3 }}><LinearProgress /></Box>}
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, lg: 5 }}>
          <Card sx={{ height: '100%' }}>
            <CardContent sx={{ p: 0 }}>
              <Box sx={{ px: 2.5, py: 2.1 }}>
                <Typography component="h2" variant="h2">Hoạt động gần đây</Typography>
                <Typography color="text.secondary" sx={{ mt: 0.35, fontSize: 12.5 }}>Theo nhật ký bảo mật của hệ thống.</Typography>
              </Box>
              <Divider />
              <Stack divider={<Divider flexItem />}>
                {dashboard.data?.recentActivities.length ? dashboard.data.recentActivities.slice(0, 4).map((activity) => (
                  <Box key={activity.id} sx={{ px: 2.5, py: 1.35 }}>
                    <Typography variant="body2" sx={{ fontWeight: 580 }}>{activityLabels[activity.eventType] ?? activity.eventType}</Typography>
                    <Typography color="text.secondary" sx={{ mt: 0.25, fontSize: 11.5 }}>{activity.actorName} · {relativeTime(activity.occurredAt)}</Typography>
                  </Box>
                )) : (
                  <Box sx={{ px: 2.5, py: 4, textAlign: 'center' }}><Typography color="text.secondary" variant="body2">Chưa có hoạt động mới.</Typography></Box>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}
