import SchoolRoundedIcon from '@mui/icons-material/SchoolRounded';
import { Box, Typography } from '@mui/material';

export function BrandMark({ compact = false }: { compact?: boolean }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
      <Box
        sx={{
          width: 38,
          height: 38,
          borderRadius: 2.5,
          bgcolor: 'primary.main',
          color: 'primary.contrastText',
          display: 'grid',
          placeItems: 'center',
        }}
      >
        <SchoolRoundedIcon fontSize="small" />
      </Box>
      {!compact && (
        <Box>
          <Typography sx={{ fontWeight: 750, lineHeight: 1.1 }}>FSchool Admin</Typography>
          <Typography variant="caption" color="text.secondary">Hệ thống quản trị</Typography>
        </Box>
      )}
    </Box>
  );
}
