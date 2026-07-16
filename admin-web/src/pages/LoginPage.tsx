import { zodResolver } from '@hookform/resolvers/zod';
import AssessmentOutlinedIcon from '@mui/icons-material/AssessmentOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import CheckCircleRoundedIcon from '@mui/icons-material/CheckCircleRounded';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import PeopleAltOutlinedIcon from '@mui/icons-material/PeopleAltOutlined';
import ShieldOutlinedIcon from '@mui/icons-material/ShieldOutlined';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import {
  Alert,
  Box,
  Button,
  Chip,
  IconButton,
  InputAdornment,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { ApiProblem } from '../api/adminAuthApi';
import { useAuth } from '../auth/authState';
import { BrandMark } from '../components/BrandMark';

const schema = z.object({
  phoneNumber: z.string().trim().min(9, 'Vui lòng nhập số điện thoại hợp lệ.'),
  password: z.string().min(1, 'Vui lòng nhập mật khẩu.'),
});

type FormValues = z.infer<typeof schema>;

function loginErrorMessage(error: unknown): string {
  if (error instanceof ApiProblem) {
    if (error.code === 'INVALID_CREDENTIALS') return 'Số điện thoại hoặc mật khẩu không đúng.';
    if (error.code === 'ADMIN_LOGIN_RATE_LIMITED') return 'Bạn đã thử đăng nhập quá nhiều lần. Vui lòng thử lại sau.';
  }
  return 'Không thể đăng nhập lúc này. Vui lòng kiểm tra kết nối và thử lại.';
}

const capabilities = [
  { icon: <PeopleAltOutlinedIcon />, title: 'Hồ sơ học sinh', detail: 'Thông tin tập trung và nhất quán' },
  { icon: <CalendarMonthOutlinedIcon />, title: 'Lịch học & hoạt động', detail: 'Theo dõi vận hành theo thời gian thực' },
  { icon: <AssessmentOutlinedIcon />, title: 'Kết quả học tập', detail: 'Dữ liệu rõ ràng và có kiểm soát' },
];

export function LoginPage() {
  const { status, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [showPassword, setShowPassword] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const { control, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { phoneNumber: '', password: '' },
  });

  if (status === 'checking') {
    return (
      <Box role="status" aria-label="Đang kiểm tra phiên đăng nhập" sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', bgcolor: '#fbfbfc' }}>
        <Typography color="text.secondary">Đang kiểm tra phiên đăng nhập…</Typography>
      </Box>
    );
  }
  if (status === 'authenticated') return <Navigate to="/" replace />;

  const onSubmit = async (values: FormValues) => {
    setSubmitError(null);
    try {
      await login(values.phoneNumber, values.password);
      const from = (location.state as { from?: string } | null)?.from ?? '/';
      navigate(from, { replace: true });
    } catch (error) {
      setSubmitError(loginErrorMessage(error));
    }
  };

  return (
    <Box component="main" sx={{ minHeight: '100vh', display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '52% 48%' }, bgcolor: '#fbfbfc' }}>
      <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', px: { xs: 3, sm: 6, xl: 10 }, py: { xs: 3, sm: 4 } }}>
        <BrandMark />
        <Box sx={{ flex: 1, display: 'grid', placeItems: 'center', py: 5 }}>
          <Box sx={{ width: '100%', maxWidth: 410 }}>
            <Chip icon={<ShieldOutlinedIcon />} label="Cổng quản trị nội bộ" size="small" sx={{ mb: 2.5, color: 'primary.dark', bgcolor: '#fff0eb' }} />
            <Typography component="h1" variant="h1" sx={{ fontSize: { xs: 28, sm: 32 } }}>Chào mừng trở lại</Typography>
            <Typography color="text.secondary" sx={{ mt: 1, mb: 3.5 }}>Đăng nhập để tiếp tục quản lý hệ thống FSchool.</Typography>

            <Stack component="form" onSubmit={handleSubmit(onSubmit)} spacing={2.25} noValidate>
              {submitError && <Alert severity="error">{submitError}</Alert>}
              <Controller name="phoneNumber" control={control} render={({ field }) => (
                <TextField
                  {...field}
                  label="Số điện thoại"
                  autoComplete="username"
                  autoFocus
                  fullWidth
                  error={Boolean(errors.phoneNumber)}
                  helperText={errors.phoneNumber?.message}
                  slotProps={{ htmlInput: { inputMode: 'tel' } }}
                />
              )} />
              <Controller name="password" control={control} render={({ field }) => (
                <TextField
                  {...field}
                  label="Mật khẩu"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  fullWidth
                  error={Boolean(errors.password)}
                  helperText={errors.password?.message}
                  slotProps={{ input: { endAdornment: (
                    <InputAdornment position="end">
                      <IconButton aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} onClick={() => setShowPassword((value) => !value)} edge="end">
                        {showPassword ? <VisibilityOffOutlinedIcon /> : <VisibilityOutlinedIcon />}
                      </IconButton>
                    </InputAdornment>
                  ) } }}
                />
              )} />
              <Button type="submit" variant="contained" size="large" disabled={isSubmitting} startIcon={!isSubmitting && <LockOutlinedIcon />} sx={{ mt: 0.5 }}>
                {isSubmitting ? 'Đang đăng nhập…' : 'Đăng nhập an toàn'}
              </Button>
            </Stack>

            <Stack direction="row" sx={{ alignItems: 'center', gap: 0.75, mt: 3, color: 'text.secondary' }}>
              <CheckCircleRoundedIcon sx={{ color: 'success.main', fontSize: 17 }} />
              <Typography variant="caption">Phiên đăng nhập được bảo vệ và tự động hết hạn.</Typography>
            </Stack>
          </Box>
        </Box>
        <Typography variant="caption" color="text.secondary">© 2026 FSchool · Chỉ dành cho nhân sự được ủy quyền</Typography>
      </Box>

      <Box sx={{ display: { xs: 'none', lg: 'flex' }, m: 2, borderRadius: 4, bgcolor: '#ed4a23', color: 'common.white', px: { lg: 7, xl: 10 }, py: 7, flexDirection: 'column', justifyContent: 'center' }}>
        <Box sx={{ maxWidth: 580 }}>
          <Typography sx={{ fontSize: 12, fontWeight: 700, letterSpacing: '0.12em', opacity: 0.78 }}>FSCHOOL OPERATIONS</Typography>
          <Typography component="p" sx={{ mt: 2, fontSize: { lg: 36, xl: 44 }, fontWeight: 740, lineHeight: 1.16, letterSpacing: '-0.035em' }}>
            Một nơi để vận hành nhà trường rõ ràng hơn.
          </Typography>
          <Typography sx={{ mt: 2, maxWidth: 500, fontSize: 15, lineHeight: 1.7, opacity: 0.82 }}>
            Theo dõi dữ liệu quan trọng, xử lý công việc và giữ mọi hoạt động học đường trong cùng một hệ thống.
          </Typography>

          <Stack spacing={1.25} sx={{ mt: 5 }}>
            {capabilities.map((item) => (
              <Stack key={item.title} direction="row" sx={{ alignItems: 'center', gap: 1.75, p: 1.5, borderRadius: 2.5, bgcolor: 'rgba(255,255,255,0.1)' }}>
                <Box sx={{ width: 38, height: 38, display: 'grid', placeItems: 'center', borderRadius: 2, bgcolor: 'rgba(255,255,255,0.14)', '& svg': { fontSize: 20 } }}>{item.icon}</Box>
                <Box>
                  <Typography sx={{ fontSize: 13.5, fontWeight: 680 }}>{item.title}</Typography>
                  <Typography sx={{ mt: 0.15, fontSize: 11.5, opacity: 0.72 }}>{item.detail}</Typography>
                </Box>
              </Stack>
            ))}
          </Stack>
        </Box>
      </Box>
    </Box>
  );
}
