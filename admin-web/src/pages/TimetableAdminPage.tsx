import AddRoundedIcon from '@mui/icons-material/AddRounded';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded';
import {
  Alert, Box, Button, Card, Chip, CircularProgress, Dialog, DialogActions,
  DialogContent, DialogTitle, IconButton, MenuItem, Stack, Table, TableBody,
  TableCell, TableHead, TableRow, TextField, Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { getAcademicCatalog } from '../api/adminAcademicsApi';
import {
  createLesson, createTimetableOverride, deleteLesson, deleteTimetableOverride,
  getAdminTimetable,
} from '../api/adminOperationsApi';
import { ApiProblem } from '../api/adminAuthApi';
import { useAuth } from '../auth/authState';

const dayNames = ['Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy', 'Chủ nhật'];

const emptyLesson = {
  dayOfWeek: 1, session: 'MORNING', periodNumber: 1, subjectId: '', teacherName: '', room: '',
};

const emptyOverride = {
  lessonDate: new Date().toISOString().slice(0, 10), session: 'MORNING', periodNumber: 1,
  overrideType: 'REPLACED', subjectId: '', teacherName: '', room: '', note: '',
};

const overrideTypeLabels: Record<string, string> = {
  REPLACED: 'Thay tiết',
  ADDED: 'Thêm tiết',
  CANCELLED: 'Hủy tiết',
};

function message(error: unknown): string {
  return error instanceof ApiProblem ? error.message : 'Không thể cập nhật thời khóa biểu.';
}

export function TimetableAdminPage() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const [termId, setTermId] = useState('');
  const [classId, setClassId] = useState('');
  const [lessonOpen, setLessonOpen] = useState(false);
  const [overrideOpen, setOverrideOpen] = useState(false);
  const [lessonForm, setLessonForm] = useState(emptyLesson);
  const [overrideForm, setOverrideForm] = useState(emptyOverride);
  const [feedback, setFeedback] = useState<string | null>(null);

  const catalogQuery = useQuery({
    queryKey: ['academic-catalog'], queryFn: () => getAcademicCatalog(accessToken!), enabled: Boolean(accessToken),
  });
  const catalog = catalogQuery.data;
  useEffect(() => {
    if (!catalog) return;
    if (!termId && catalog.terms.length) setTermId(catalog.terms[0].id);
    if (!classId && catalog.classes.length) setClassId(catalog.classes[0].id);
  }, [catalog, termId, classId]);

  const timetableQuery = useQuery({
    queryKey: ['admin-timetable', termId, classId],
    queryFn: () => getAdminTimetable(accessToken!, termId, classId),
    enabled: Boolean(accessToken && termId && classId),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['admin-timetable', termId, classId] });
  const lessonMutation = useMutation({
    mutationFn: () => createLesson(accessToken!, {
      academicTermId: termId, schoolClassId: classId, ...lessonForm,
      dayOfWeek: Number(lessonForm.dayOfWeek), periodNumber: Number(lessonForm.periodNumber),
    }),
    onSuccess: async () => { await invalidate(); setLessonOpen(false); setLessonForm(emptyLesson); setFeedback('Đã thêm tiết học.'); },
  });
  const overrideMutation = useMutation({
    mutationFn: () => createTimetableOverride(accessToken!, {
      academicTermId: termId, schoolClassId: classId, ...overrideForm,
      periodNumber: Number(overrideForm.periodNumber),
      subjectId: overrideForm.overrideType === 'CANCELLED' ? null : overrideForm.subjectId,
    }),
    onSuccess: async () => { await invalidate(); setOverrideOpen(false); setOverrideForm(emptyOverride); setFeedback('Đã lưu thay đổi theo ngày.'); },
  });
  const removeLesson = useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) => deleteLesson(accessToken!, id, version),
    onSuccess: invalidate,
  });
  const removeOverride = useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) => deleteTimetableOverride(accessToken!, id, version),
    onSuccess: invalidate,
  });

  const lessonsByDay = useMemo(() => dayNames.map((_, index) =>
    timetableQuery.data?.lessons.filter((lesson) => lesson.dayOfWeek === index + 1) ?? []), [timetableQuery.data]);

  return (
    <Stack spacing={2.5}>
      <Box sx={{ display: 'flex', alignItems: { xs: 'flex-start', sm: 'center' }, justifyContent: 'space-between', gap: 2, flexWrap: 'wrap' }}>
        <Box><Typography variant="h4">Lịch học</Typography><Typography color="text.secondary">Thiết lập lịch tuần 45 phút và thay đổi theo ngày.</Typography></Box>
        <Stack direction="row" spacing={1}>
          <Button variant="outlined" startIcon={<CalendarMonthOutlinedIcon />} onClick={() => setOverrideOpen(true)} disabled={!termId || !classId}>Thay đổi theo ngày</Button>
          <Button variant="contained" startIcon={<AddRoundedIcon />} onClick={() => setLessonOpen(true)} disabled={!termId || !classId}>Thêm tiết học</Button>
        </Stack>
      </Box>
      {feedback && <Alert severity="success" onClose={() => setFeedback(null)}>{feedback}</Alert>}
      {(lessonMutation.error || overrideMutation.error || removeLesson.error || removeOverride.error) &&
        <Alert severity="error">{message(lessonMutation.error ?? overrideMutation.error ?? removeLesson.error ?? removeOverride.error)}</Alert>}
      <Card sx={{ p: 2 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
          <TextField select label="Học kỳ" value={termId} onChange={(event) => setTermId(event.target.value)} sx={{ minWidth: 240 }}>
            {catalog?.terms.map((term) => <MenuItem key={term.id} value={term.id}>{term.name} · {term.code}</MenuItem>)}
          </TextField>
          <TextField select label="Lớp" value={classId} onChange={(event) => setClassId(event.target.value)} sx={{ minWidth: 180 }}>
            {catalog?.classes.filter((item) => item.enabled).map((item) => <MenuItem key={item.id} value={item.id}>{item.code}</MenuItem>)}
          </TextField>
          <Box sx={{ flex: 1 }} />
          <Chip label="Mỗi tiết 45 phút" color="primary" variant="outlined" />
        </Stack>
      </Card>
      {timetableQuery.isLoading ? <Box sx={{ py: 8, textAlign: 'center' }}><CircularProgress /></Box> : (
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: 'repeat(3, 1fr)', xl: 'repeat(6, 1fr)' }, gap: 1.5 }}>
          {dayNames.slice(0, 6).map((day, index) => <Card key={day} variant="outlined" sx={{ p: 1.5, minHeight: 180 }}>
            <Typography sx={{ fontWeight: 700, mb: 1.25 }}>{day}</Typography>
            <Stack spacing={1}>
              {lessonsByDay[index].map((lesson) => <Box key={lesson.id} sx={{ borderRadius: 2, bgcolor: '#fff4ef', p: 1.25, position: 'relative' }}>
                <Typography sx={{ fontSize: 13, fontWeight: 700 }}>{lesson.subjectName}</Typography>
                <Typography variant="caption" color="text.secondary">{lesson.session === 'MORNING' ? 'Sáng' : 'Chiều'} · Tiết {lesson.periodNumber}</Typography>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>{lesson.room || 'Chưa có phòng'}{lesson.teacherName ? ` · ${lesson.teacherName}` : ''}</Typography>
                <IconButton size="small" aria-label={`Xóa tiết ${lesson.subjectName}`} onClick={() => removeLesson.mutate(lesson)} sx={{ position: 'absolute', top: 2, right: 2 }}><DeleteOutlineRoundedIcon fontSize="small" /></IconButton>
              </Box>)}
              {!lessonsByDay[index].length && <Typography variant="body2" color="text.secondary">Chưa có tiết học</Typography>}
            </Stack>
          </Card>)}
        </Box>
      )}
      <Card variant="outlined">
        <Box sx={{ px: 2, py: 1.5 }}><Typography sx={{ fontWeight: 700 }}>Thay đổi gần đây</Typography></Box>
        <Table size="small"><TableHead><TableRow><TableCell>Ngày</TableCell><TableCell>Tiết</TableCell><TableCell>Loại</TableCell><TableCell>Nội dung</TableCell><TableCell align="right">Thao tác</TableCell></TableRow></TableHead>
          <TableBody>{timetableQuery.data?.overrides.map((item) => <TableRow key={item.id}><TableCell>{new Intl.DateTimeFormat('vi-VN').format(new Date(`${item.lessonDate}T00:00:00`))}</TableCell><TableCell>{item.session === 'MORNING' ? 'Sáng' : 'Chiều'} · {item.periodNumber}</TableCell><TableCell><Chip size="small" label={overrideTypeLabels[item.overrideType] ?? 'Không xác định'} /></TableCell><TableCell>{item.subjectName ?? item.note ?? 'Hủy tiết'}</TableCell><TableCell align="right"><IconButton aria-label="Xóa thay đổi" onClick={() => removeOverride.mutate(item)}><DeleteOutlineRoundedIcon fontSize="small" /></IconButton></TableCell></TableRow>)}</TableBody>
        </Table>
      </Card>

      <Dialog open={lessonOpen} onClose={() => setLessonOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Thêm tiết học</DialogTitle><DialogContent><Stack spacing={2} sx={{ pt: 1 }}>
          <TextField select label="Ngày" value={lessonForm.dayOfWeek} onChange={(event) => setLessonForm({ ...lessonForm, dayOfWeek: Number(event.target.value) })}>{dayNames.map((day, index) => <MenuItem key={day} value={index + 1}>{day}</MenuItem>)}</TextField>
          <Stack direction="row" spacing={1.5}><TextField select fullWidth label="Buổi" value={lessonForm.session} onChange={(event) => setLessonForm({ ...lessonForm, session: event.target.value })}><MenuItem value="MORNING">Sáng</MenuItem><MenuItem value="AFTERNOON">Chiều</MenuItem></TextField><TextField select fullWidth label="Tiết" value={lessonForm.periodNumber} onChange={(event) => setLessonForm({ ...lessonForm, periodNumber: Number(event.target.value) })}>{[1,2,3,4,5].map((value) => <MenuItem key={value} value={value}>{value}</MenuItem>)}</TextField></Stack>
          <TextField select label="Môn học" value={lessonForm.subjectId} onChange={(event) => setLessonForm({ ...lessonForm, subjectId: event.target.value })}>{catalog?.subjects.filter((item) => item.enabled).map((item) => <MenuItem key={item.id} value={item.id}>{item.name}</MenuItem>)}</TextField>
          <TextField label="Giáo viên" value={lessonForm.teacherName} onChange={(event) => setLessonForm({ ...lessonForm, teacherName: event.target.value })} /><TextField label="Phòng học" value={lessonForm.room} onChange={(event) => setLessonForm({ ...lessonForm, room: event.target.value })} />
        </Stack></DialogContent><DialogActions><Button onClick={() => setLessonOpen(false)}>Hủy</Button><Button variant="contained" disabled={!lessonForm.subjectId || lessonMutation.isPending} onClick={() => lessonMutation.mutate()}>Lưu tiết học</Button></DialogActions>
      </Dialog>
      <Dialog open={overrideOpen} onClose={() => setOverrideOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Thay đổi lịch theo ngày</DialogTitle><DialogContent><Stack spacing={2} sx={{ pt: 1 }}>
          <TextField type="date" label="Ngày áp dụng" value={overrideForm.lessonDate} onChange={(event) => setOverrideForm({ ...overrideForm, lessonDate: event.target.value })} slotProps={{ inputLabel: { shrink: true } }} />
          <TextField select label="Loại thay đổi" value={overrideForm.overrideType} onChange={(event) => setOverrideForm({ ...overrideForm, overrideType: event.target.value })}><MenuItem value="REPLACED">Thay tiết</MenuItem><MenuItem value="ADDED">Thêm tiết</MenuItem><MenuItem value="CANCELLED">Hủy tiết</MenuItem></TextField>
          <Stack direction="row" spacing={1.5}><TextField select fullWidth label="Buổi" value={overrideForm.session} onChange={(event) => setOverrideForm({ ...overrideForm, session: event.target.value })}><MenuItem value="MORNING">Sáng</MenuItem><MenuItem value="AFTERNOON">Chiều</MenuItem></TextField><TextField select fullWidth label="Tiết" value={overrideForm.periodNumber} onChange={(event) => setOverrideForm({ ...overrideForm, periodNumber: Number(event.target.value) })}>{[1,2,3,4,5].map((value) => <MenuItem key={value} value={value}>{value}</MenuItem>)}</TextField></Stack>
          {overrideForm.overrideType !== 'CANCELLED' && <TextField select label="Môn học" value={overrideForm.subjectId} onChange={(event) => setOverrideForm({ ...overrideForm, subjectId: event.target.value })}>{catalog?.subjects.filter((item) => item.enabled).map((item) => <MenuItem key={item.id} value={item.id}>{item.name}</MenuItem>)}</TextField>}
          <TextField label="Ghi chú" value={overrideForm.note} onChange={(event) => setOverrideForm({ ...overrideForm, note: event.target.value })} />
        </Stack></DialogContent><DialogActions><Button onClick={() => setOverrideOpen(false)}>Hủy</Button><Button variant="contained" disabled={(overrideForm.overrideType !== 'CANCELLED' && !overrideForm.subjectId) || overrideMutation.isPending} onClick={() => overrideMutation.mutate()}>Lưu thay đổi</Button></DialogActions>
      </Dialog>
    </Stack>
  );
}
