import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded';
import { Box, Button, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';

export function NotFoundPage() {
  const navigate = useNavigate();
  return (
    <Box sx={{ minHeight: '60vh', display: 'grid', placeItems: 'center', textAlign: 'center' }}>
      <Box>
        <Typography variant="h1">Không tìm thấy trang</Typography>
        <Typography color="text.secondary" sx={{ mt: 1, mb: 3 }}>
          Đường dẫn này không tồn tại trong hệ thống quản trị.
        </Typography>
        <Button startIcon={<ArrowBackRoundedIcon />} variant="outlined" onClick={() => navigate('/')}>
          Về tổng quan
        </Button>
      </Box>
    </Box>
  );
}
