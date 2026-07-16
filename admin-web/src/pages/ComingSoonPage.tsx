import ConstructionRoundedIcon from '@mui/icons-material/ConstructionRounded';
import { Box, Card, CardContent, Chip, Stack, Typography } from '@mui/material';

export function ComingSoonPage({ title, description }: { title: string; description: string }) {
  return (
    <Box sx={{ maxWidth: 1180, mx: 'auto' }}>
      <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
        <Box>
          <Typography component="h1" variant="h1">{title}</Typography>
          <Typography color="text.secondary" sx={{ mt: 0.5 }}>{description}</Typography>
        </Box>
        <Chip label="Sắp triển khai" size="small" />
      </Stack>
      <Card>
        <CardContent sx={{ py: 10, textAlign: 'center' }}>
          <Box sx={{ color: 'primary.main', mb: 1.5 }}><ConstructionRoundedIcon /></Box>
          <Typography component="h2" variant="h2">Module đang được chuẩn bị</Typography>
          <Typography color="text.secondary" sx={{ mt: 0.75 }}>
            Nền tảng và điều hướng đã sẵn sàng cho phase tiếp theo.
          </Typography>
        </CardContent>
      </Card>
    </Box>
  );
}
