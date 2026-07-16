import CheckRoundedIcon from '@mui/icons-material/CheckRounded';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import {
  Alert, Box, Button, Card, Chip, CircularProgress, MenuItem, Stack, Table,
  TableBody, TableCell, TableHead, TableRow, TextField, Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { getAdminForms, updateAdminFormStatus, type StudentForm } from '../api/adminOperationsApi';
import { ApiProblem } from '../api/adminAuthApi';
import { useAuth } from '../auth/authState';

const statusLabels: Record<string, string> = { SUBMITTED: 'Mới gửi', IN_REVIEW: 'Đang xử lý', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối', CANCELLED: 'Đã hủy' };
const typeLabels: Record<string, string> = { LEAVE_OF_ABSENCE: 'Đơn xin nghỉ học', STUDENT_CONFIRMATION: 'Xác nhận học sinh', TRANSCRIPT_REQUEST: 'Xin bảng điểm', STUDENT_CARD_REISSUE: 'Cấp lại thẻ học sinh' };

export function FormsAdminPage() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const [status, setStatus] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const formsQuery = useQuery({ queryKey: ['admin-forms', status], queryFn: () => getAdminForms(accessToken!, status || undefined), enabled: Boolean(accessToken) });
  const mutation = useMutation({
    mutationFn: ({ form, nextStatus }: { form: StudentForm; nextStatus: string }) => updateAdminFormStatus(accessToken!, form.id, nextStatus, form.version),
    onSuccess: async (_, variables) => { await queryClient.invalidateQueries({ queryKey: ['admin-forms'] }); setFeedback(variables.nextStatus === 'APPROVED' ? 'Đã duyệt đơn và cập nhật lịch sử.' : 'Đã cập nhật trạng thái đơn.'); },
  });
  const error = mutation.error instanceof ApiProblem ? mutation.error.message : mutation.error ? 'Không thể cập nhật đơn.' : null;
  return <Stack spacing={2.5}>
    <Box><Typography variant="h4">Đơn từ</Typography><Typography color="text.secondary">Tiếp nhận, theo dõi và phê duyệt yêu cầu của học sinh.</Typography></Box>
    {feedback && <Alert severity="success" onClose={() => setFeedback(null)}>{feedback}</Alert>}{error && <Alert severity="error">{error}</Alert>}
    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' }, gap: 1.5 }}>
      <Card sx={{ p: 2 }}><Typography variant="caption" color="text.secondary">Tổng đơn</Typography><Typography variant="h5">{formsQuery.data?.length ?? 0}</Typography></Card>
      <Card sx={{ p: 2 }}><Typography variant="caption" color="text.secondary">Cần xử lý</Typography><Typography variant="h5" color="primary.main">{formsQuery.data?.filter((item) => ['SUBMITTED','IN_REVIEW'].includes(item.status)).length ?? 0}</Typography></Card>
      <Card sx={{ p: 2 }}><Typography variant="caption" color="text.secondary">Đã hoàn tất</Typography><Typography variant="h5">{formsQuery.data?.filter((item) => ['APPROVED','REJECTED'].includes(item.status)).length ?? 0}</Typography></Card>
    </Box>
    <Card variant="outlined">
      <Box sx={{ px: 2, py: 1.5, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2 }}><Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}><DescriptionOutlinedIcon color="primary" /><Typography sx={{ fontWeight: 700 }}>Danh sách đơn</Typography></Stack><TextField select size="small" label="Trạng thái" value={status} onChange={(event) => setStatus(event.target.value)} sx={{ minWidth: 180 }}><MenuItem value="">Tất cả</MenuItem>{Object.entries(statusLabels).map(([value,label]) => <MenuItem key={value} value={value}>{label}</MenuItem>)}</TextField></Box>
      {formsQuery.isLoading ? <Box sx={{ py: 8, textAlign: 'center' }}><CircularProgress /></Box> : <Table size="small"><TableHead><TableRow><TableCell>Học sinh</TableCell><TableCell>Loại đơn</TableCell><TableCell>Lý do</TableCell><TableCell>Ngày gửi</TableCell><TableCell>Trạng thái</TableCell><TableCell align="right">Xử lý</TableCell></TableRow></TableHead><TableBody>
        {formsQuery.data?.map((form) => <TableRow key={form.id}><TableCell><Typography variant="body2" sx={{ fontWeight: 650 }}>{form.studentName}</Typography><Typography variant="caption" color="text.secondary">{form.studentCode}</Typography></TableCell><TableCell>{typeLabels[form.formType] ?? form.formType}</TableCell><TableCell sx={{ maxWidth: 280 }}><Typography variant="body2" noWrap>{form.reason}</Typography></TableCell><TableCell>{new Intl.DateTimeFormat('vi-VN').format(new Date(form.submittedAt))}</TableCell><TableCell><Chip size="small" color={form.status === 'APPROVED' ? 'success' : form.status === 'REJECTED' ? 'error' : 'warning'} label={statusLabels[form.status] ?? form.status} /></TableCell><TableCell align="right"><Stack direction="row" spacing={0.5} sx={{ justifyContent: 'flex-end' }}><Button size="small" startIcon={<CheckRoundedIcon />} disabled={form.status === 'APPROVED'} onClick={() => mutation.mutate({ form, nextStatus: 'APPROVED' })}>Duyệt</Button><Button size="small" color="error" startIcon={<CloseRoundedIcon />} disabled={form.status === 'REJECTED'} onClick={() => mutation.mutate({ form, nextStatus: 'REJECTED' })}>Từ chối</Button></Stack></TableCell></TableRow>)}
      </TableBody></Table>}
    </Card>
  </Stack>;
}
