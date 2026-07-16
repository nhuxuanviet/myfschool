import AddRoundedIcon from '@mui/icons-material/AddRounded';
import CampaignOutlinedIcon from '@mui/icons-material/CampaignOutlined';
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import {
  Alert, Box, Button, Card, Chip, Dialog, DialogActions, DialogContent,
  DialogTitle, IconButton, MenuItem, Stack, TextField, Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import {
  createAnnouncement, deleteAnnouncement, getAnnouncements, updateAnnouncement,
  type Announcement,
} from '../api/adminOperationsApi';
import { ApiProblem } from '../api/adminAuthApi';
import { useAuth } from '../auth/authState';

function localDateTime(value: Date): string {
  const local = new Date(value.getTime() - value.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 16);
}

const initialForm = { title: '', body: '', audience: 'ALL', audienceGradeLevel: 10, publishedAt: localDateTime(new Date()), visibleFrom: localDateTime(new Date()), visibleUntil: '' };

export function NotificationsAdminPage() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Announcement | null>(null);
  const [form, setForm] = useState(initialForm);
  const [feedback, setFeedback] = useState<string | null>(null);
  const query = useQuery({ queryKey: ['admin-announcements'], queryFn: () => getAnnouncements(accessToken!), enabled: Boolean(accessToken) });
  const save = useMutation({
    mutationFn: async () => {
      const input = { ...form, audienceGradeLevel: form.audience === 'GRADE' ? Number(form.audienceGradeLevel) : null, publishedAt: new Date(form.publishedAt).toISOString(), visibleFrom: new Date(form.visibleFrom).toISOString(), visibleUntil: form.visibleUntil ? new Date(form.visibleUntil).toISOString() : null, ...(editing ? { version: editing.version } : {}) };
      if (editing) await updateAnnouncement(accessToken!, editing.id, input); else await createAnnouncement(accessToken!, input);
    },
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['admin-announcements'] }); setOpen(false); setEditing(null); setForm(initialForm); setFeedback('Thông báo đã được xuất bản cho ứng dụng học sinh.'); },
  });
  const remove = useMutation({ mutationFn: (item: Announcement) => deleteAnnouncement(accessToken!, item.id, item.version), onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-announcements'] }) });
  const edit = (item: Announcement) => { setEditing(item); setForm({ title: item.title, body: item.body, audience: item.audience, audienceGradeLevel: item.audienceGradeLevel ?? 10, publishedAt: localDateTime(new Date(item.publishedAt)), visibleFrom: localDateTime(new Date(item.visibleFrom)), visibleUntil: item.visibleUntil ? localDateTime(new Date(item.visibleUntil)) : '' }); setOpen(true); };
  const error = save.error ?? remove.error;
  return <Stack spacing={2.5}>
    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 2 }}><Box><Typography variant="h4">Thông báo</Typography><Typography color="text.secondary">Xuất bản nội dung đến toàn trường hoặc theo khối.</Typography></Box><Button variant="contained" startIcon={<AddRoundedIcon />} onClick={() => { setEditing(null); setForm(initialForm); setOpen(true); }}>Soạn thông báo</Button></Box>
    {feedback && <Alert severity="success" onClose={() => setFeedback(null)}>{feedback}</Alert>}{error && <Alert severity="error">{error instanceof ApiProblem ? error.message : 'Không thể cập nhật thông báo.'}</Alert>}
    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: 'repeat(2, 1fr)' }, gap: 1.5 }}>{query.data?.map((item) => <Card variant="outlined" key={item.id} sx={{ p: 2 }}><Stack direction="row" spacing={1.5} sx={{ alignItems: 'flex-start' }}><Box sx={{ width: 42, height: 42, display: 'grid', placeItems: 'center', borderRadius: 2, bgcolor: '#fff0eb', color: 'primary.main' }}><CampaignOutlinedIcon /></Box><Box sx={{ flex: 1, minWidth: 0 }}><Stack direction="row" sx={{ justifyContent: 'space-between', gap: 1 }}><Typography sx={{ fontWeight: 750 }}>{item.title}</Typography><Chip size="small" label={item.audience === 'ALL' ? 'Toàn trường' : `Khối ${item.audienceGradeLevel}`} /></Stack><Typography variant="body2" color="text.secondary" sx={{ mt: 0.75, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>{item.body}</Typography><Typography variant="caption" color="text.secondary">Xuất bản {new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(item.publishedAt))}</Typography></Box><Stack><IconButton aria-label={`Sửa thông báo ${item.title}`} onClick={() => edit(item)}><EditOutlinedIcon fontSize="small" /></IconButton><IconButton aria-label={`Xóa thông báo ${item.title}`} color="error" onClick={() => remove.mutate(item)}><DeleteOutlineRoundedIcon fontSize="small" /></IconButton></Stack></Stack></Card>)}</Box>
    <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm"><DialogTitle>{editing ? 'Cập nhật thông báo' : 'Soạn thông báo'}</DialogTitle><DialogContent><Stack spacing={2} sx={{ pt: 1 }}><TextField label="Tiêu đề" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} /><TextField label="Nội dung" multiline minRows={4} value={form.body} onChange={(event) => setForm({ ...form, body: event.target.value })} /><TextField select label="Đối tượng" value={form.audience} onChange={(event) => setForm({ ...form, audience: event.target.value })}><MenuItem value="ALL">Toàn trường</MenuItem><MenuItem value="GRADE">Theo khối</MenuItem></TextField>{form.audience === 'GRADE' && <TextField select label="Khối" value={form.audienceGradeLevel} onChange={(event) => setForm({ ...form, audienceGradeLevel: Number(event.target.value) })}>{[6,7,8,9,10,11,12].map((grade) => <MenuItem key={grade} value={grade}>Khối {grade}</MenuItem>)}</TextField>}<Stack direction="row" spacing={1.5}><TextField fullWidth type="datetime-local" label="Xuất bản lúc" value={form.publishedAt} onChange={(event) => setForm({ ...form, publishedAt: event.target.value })} slotProps={{ inputLabel: { shrink: true } }} /><TextField fullWidth type="datetime-local" label="Hiển thị từ" value={form.visibleFrom} onChange={(event) => setForm({ ...form, visibleFrom: event.target.value })} slotProps={{ inputLabel: { shrink: true } }} /></Stack><TextField type="datetime-local" label="Ẩn sau (không bắt buộc)" value={form.visibleUntil} onChange={(event) => setForm({ ...form, visibleUntil: event.target.value })} slotProps={{ inputLabel: { shrink: true } }} /></Stack></DialogContent><DialogActions><Button onClick={() => setOpen(false)}>Hủy</Button><Button variant="contained" disabled={!form.title.trim() || !form.body.trim() || save.isPending} onClick={() => save.mutate()}>{editing ? 'Lưu thay đổi' : 'Xuất bản'}</Button></DialogActions></Dialog>
  </Stack>;
}
