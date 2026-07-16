import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';
import CheckCircleOutlineRoundedIcon from '@mui/icons-material/CheckCircleOutlineRounded';
import KeyOffRoundedIcon from '@mui/icons-material/KeyOffRounded';
import MemoryRoundedIcon from '@mui/icons-material/MemoryRounded';
import SecurityRoundedIcon from '@mui/icons-material/SecurityRounded';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  Grid,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { getAdminAiSettings, updateAdminAiSettings } from '../api/adminAiSettingsApi';
import { ApiProblem } from '../api/adminAuthApi';
import { useAuth } from '../auth/authState';

interface SettingsForm {
  model: string;
  temperature: string;
  maxCompletionTokens: string;
  memoryMaxMessages: string;
}

const emptyForm: SettingsForm = {
  model: '',
  temperature: '0.6',
  maxCompletionTokens: '800',
  memoryMaxMessages: '12',
};

const statusCopy = {
  READY: { label: 'Sẵn sàng', color: 'success' as const },
  MISSING_API_KEY: { label: 'Thiếu API key', color: 'warning' as const },
  LOCAL_FALLBACK: { label: 'Chế độ nội bộ', color: 'info' as const },
  UNSUPPORTED_PROVIDER: { label: 'Provider không hỗ trợ', color: 'error' as const },
};

export function AiSettingsPage() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<SettingsForm>(emptyForm);
  const [success, setSuccess] = useState(false);
  const settingsQuery = useQuery({
    queryKey: ['admin-ai-settings'],
    queryFn: () => getAdminAiSettings(accessToken!),
    enabled: Boolean(accessToken),
  });

  useEffect(() => {
    if (!settingsQuery.data) return;
    setForm({
      model: settingsQuery.data.model,
      temperature: String(settingsQuery.data.temperature),
      maxCompletionTokens: String(settingsQuery.data.maxCompletionTokens),
      memoryMaxMessages: String(settingsQuery.data.memoryMaxMessages),
    });
  }, [settingsQuery.data]);

  const updateMutation = useMutation({
    mutationFn: () => updateAdminAiSettings(accessToken!, {
      model: form.model.trim(),
      temperature: Number(form.temperature),
      maxCompletionTokens: Number(form.maxCompletionTokens),
      memoryMaxMessages: Number(form.memoryMaxMessages),
      version: settingsQuery.data!.version,
    }),
    onSuccess: (settings) => {
      queryClient.setQueryData(['admin-ai-settings'], settings);
      setSuccess(true);
    },
  });

  const validationError = validate(form);
  const status = settingsQuery.data
    ? statusCopy[settingsQuery.data.status] ?? statusCopy.UNSUPPORTED_PROVIDER
    : null;

  if (settingsQuery.isLoading) {
    return <Box role="status" aria-label="Đang tải cấu hình AI" sx={{ py: 12, textAlign: 'center' }}><CircularProgress /></Box>;
  }
  if (settingsQuery.isError || !settingsQuery.data) {
    return <Alert severity="error" action={<Button onClick={() => void settingsQuery.refetch()}>Thử lại</Button>}>Không thể tải cấu hình AI.</Alert>;
  }

  return (
    <Stack spacing={2.5}>
      <Box>
        <Typography variant="h4">Cấu hình AI</Typography>
        <Typography color="text.secondary">Quản lý model và giới hạn vận hành của trợ lý học sinh.</Typography>
      </Box>

      {success && <Alert severity="success" onClose={() => setSuccess(false)}>Đã cập nhật cấu hình AI.</Alert>}
      {updateMutation.isError && (
        <Alert severity="error">
          {updateMutation.error instanceof ApiProblem && updateMutation.error.status === 409
            ? 'Cấu hình vừa được thay đổi ở phiên khác. Hãy tải lại rồi thử lại.'
            : 'Không thể lưu cấu hình AI. Vui lòng kiểm tra dữ liệu và thử lại.'}
        </Alert>
      )}
      {settingsQuery.data.status === 'MISSING_API_KEY' && (
        <Alert severity="warning" icon={<KeyOffRoundedIcon />}>
          OpenAI đang được chọn nhưng backend chưa nhận OPENAI_API_KEY. Hãy cấu hình secret ở môi trường triển khai rồi khởi động lại backend.
        </Alert>
      )}

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 4 }}>
          <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent>
              <Stack direction="row" spacing={1} sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
                <Box><Typography variant="overline" color="text.secondary">Provider</Typography><Typography variant="h6">{settingsQuery.data.provider}</Typography></Box>
                <AutoAwesomeRoundedIcon color="primary" />
              </Stack>
              <Chip sx={{ mt: 2 }} color={status?.color} label={status?.label} size="small" />
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent>
              <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
                <Box><Typography variant="overline" color="text.secondary">Secret backend</Typography><Typography variant="h6">{settingsQuery.data.apiKeyConfigured ? 'Đã cấu hình' : 'Chưa cấu hình'}</Typography></Box>
                {settingsQuery.data.apiKeyConfigured ? <CheckCircleOutlineRoundedIcon color="success" /> : <KeyOffRoundedIcon color="warning" />}
              </Stack>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>Khóa API không bao giờ được gửi tới trình duyệt hoặc lưu trong PostgreSQL.</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent>
              <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
                <Box><Typography variant="overline" color="text.secondary">Bộ nhớ hội thoại</Typography><Typography variant="h6">{settingsQuery.data.memoryMaxMessages} tin nhắn</Typography></Box>
                <MemoryRoundedIcon color="primary" />
              </Stack>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>Bộ nhớ được tách biệt theo học sinh và mã hội thoại.</Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Card variant="outlined">
        <CardContent>
          <Stack spacing={2.5}>
            <Box><Typography variant="h6">Thiết lập phản hồi</Typography><Typography variant="body2" color="text.secondary">Các thay đổi áp dụng cho lượt hỏi tiếp theo và được ghi vào nhật ký quản trị.</Typography></Box>
            <Divider />
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, md: 6 }}><TextField fullWidth label="Model" value={form.model} onChange={(event) => setForm((value) => ({ ...value, model: event.target.value }))} helperText="Ví dụ: gpt-5.6-luna" /></Grid>
              <Grid size={{ xs: 12, md: 6 }}><TextField fullWidth label="Temperature" type="number" value={form.temperature} onChange={(event) => setForm((value) => ({ ...value, temperature: event.target.value }))} slotProps={{ htmlInput: { min: 0, max: 2, step: 0.1 } }} helperText="0 tập trung hơn; tối đa 2 theo OpenAI API." /></Grid>
              <Grid size={{ xs: 12, md: 6 }}><TextField fullWidth label="Token phản hồi tối đa" type="number" value={form.maxCompletionTokens} onChange={(event) => setForm((value) => ({ ...value, maxCompletionTokens: event.target.value }))} slotProps={{ htmlInput: { min: 100, max: 4000 } }} /></Grid>
              <Grid size={{ xs: 12, md: 6 }}><TextField fullWidth label="Số tin nhắn ghi nhớ" type="number" value={form.memoryMaxMessages} onChange={(event) => setForm((value) => ({ ...value, memoryMaxMessages: event.target.value }))} slotProps={{ htmlInput: { min: 2, max: 30 } }} /></Grid>
            </Grid>
            {validationError && <Alert severity="error">{validationError}</Alert>}
            <Box><Button variant="contained" disabled={Boolean(validationError) || updateMutation.isPending} onClick={() => updateMutation.mutate()}>{updateMutation.isPending ? 'Đang lưu…' : 'Lưu cấu hình'}</Button></Box>
          </Stack>
        </CardContent>
      </Card>

      <Alert severity="info" icon={<SecurityRoundedIcon />}>
        System prompt và danh sách tool đọc dữ liệu học sinh được cố định trong backend để tránh chỉnh sửa ngoài kiểm soát. AI không có tool ghi điểm, duyệt đơn hoặc thay đổi dữ liệu nhà trường.
      </Alert>
    </Stack>
  );
}

function validate(form: SettingsForm): string | null {
  if (!/^[A-Za-z0-9][A-Za-z0-9._:-]{2,79}$/.test(form.model.trim())) return 'Tên model không hợp lệ.';
  const temperature = Number(form.temperature);
  if (!Number.isFinite(temperature) || temperature < 0 || temperature > 2) return 'Temperature phải nằm trong khoảng 0 đến 2.';
  const tokens = Number(form.maxCompletionTokens);
  if (!Number.isInteger(tokens) || tokens < 100 || tokens > 4000) return 'Token phản hồi phải nằm trong khoảng 100 đến 4000.';
  const memory = Number(form.memoryMaxMessages);
  if (!Number.isInteger(memory) || memory < 2 || memory > 30) return 'Bộ nhớ phải nằm trong khoảng 2 đến 30 tin nhắn.';
  return null;
}
