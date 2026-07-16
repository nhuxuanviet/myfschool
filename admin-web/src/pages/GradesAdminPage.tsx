import AddRoundedIcon from '@mui/icons-material/AddRounded';
import AssessmentOutlinedIcon from '@mui/icons-material/AssessmentOutlined';
import {
  Alert, Box, Button, Card, Chip, CircularProgress, Dialog, DialogActions,
  DialogContent, DialogTitle, MenuItem, Stack, TextField, Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { getAcademicCatalog, getStudents } from '../api/adminAcademicsApi';
import { assignGradeSubject, createGradeAssessment, getAdminGrades } from '../api/adminOperationsApi';
import { ApiProblem } from '../api/adminAuthApi';
import { useAuth } from '../auth/authState';

const initialAssessment = {
  studentTermSubjectId: '', assessmentKind: 'REGULAR', assessmentForm: 'WRITTEN',
  displayLabel: '15 phút', durationMinutes: 15, status: 'RECORDED', score: '', assessedOn: new Date().toISOString().slice(0, 10), displayOrder: 1,
};

const assessmentKindLabels: Record<string, string> = {
  REGULAR: 'Thường xuyên', MIDTERM: 'Giữa kỳ', FINAL: 'Cuối kỳ',
};
const assessmentFormLabels: Record<string, string> = {
  ORAL: 'Miệng', WRITTEN: 'Viết', PRESENTATION: 'Thuyết trình', PRACTICAL: 'Thực hành',
};
const assessmentOutcomeLabels: Record<string, string> = {
  ACHIEVED: 'Đạt', NOT_ACHIEVED: 'Chưa đạt',
};

function formatAssessmentResult(score: number | null, outcome: string | null): string | number {
  if (score != null) return score;
  if (outcome == null) return '—';
  return assessmentOutcomeLabels[outcome] ?? 'Chưa xác định';
}

function errorMessage(error: unknown): string {
  return error instanceof ApiProblem ? error.message : 'Không thể cập nhật điểm số.';
}

export function GradesAdminPage() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const [studentId, setStudentId] = useState('');
  const [termId, setTermId] = useState('');
  const [assessmentOpen, setAssessmentOpen] = useState(false);
  const [subjectOpen, setSubjectOpen] = useState(false);
  const [assessment, setAssessment] = useState(initialAssessment);
  const [subjectForm, setSubjectForm] = useState({ subjectId: '', assessmentMode: 'NUMERIC', annualLessonCount: 70, displayOrder: 10 });
  const [feedback, setFeedback] = useState<string | null>(null);

  const catalogQuery = useQuery({ queryKey: ['academic-catalog'], queryFn: () => getAcademicCatalog(accessToken!), enabled: Boolean(accessToken) });
  const studentsQuery = useQuery({ queryKey: ['students-for-grades'], queryFn: () => getStudents(accessToken!, { page: 0, size: 100, sort: 'fullName,asc', enabled: true }), enabled: Boolean(accessToken) });
  useEffect(() => {
    if (!studentId && studentsQuery.data?.items.length) setStudentId(studentsQuery.data.items[0].id);
    if (!termId && catalogQuery.data?.terms.length) setTermId(catalogQuery.data.terms[0].id);
  }, [studentId, termId, studentsQuery.data, catalogQuery.data]);
  const gradesQuery = useQuery({ queryKey: ['admin-grades', studentId, termId], queryFn: () => getAdminGrades(accessToken!, studentId, termId), enabled: Boolean(accessToken && studentId && termId) });
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['admin-grades', studentId, termId] });
  const subjectMutation = useMutation({
    mutationFn: () => assignGradeSubject(accessToken!, { studentId, academicTermId: termId, ...subjectForm, annualLessonCount: subjectForm.assessmentMode === 'NUMERIC' ? Number(subjectForm.annualLessonCount) : null, displayOrder: Number(subjectForm.displayOrder) }),
    onSuccess: async () => { await invalidate(); setSubjectOpen(false); setFeedback('Đã thêm môn học cho học sinh.'); },
  });
  const assessmentMutation = useMutation({
    mutationFn: () => createGradeAssessment(accessToken!, { ...assessment, durationMinutes: Number(assessment.durationMinutes) || null, score: assessment.status === 'RECORDED' ? Number(assessment.score) : null, outcome: null, displayOrder: Number(assessment.displayOrder) }),
    onSuccess: async () => { await invalidate(); setAssessmentOpen(false); setAssessment(initialAssessment); setFeedback('Đã ghi nhận điểm thành phần.'); },
  });
  const assessmentsBySubject = useMemo(() => new Map(gradesQuery.data?.subjects.map((subject) => [subject.id, gradesQuery.data?.assessments.filter((item) => item.studentTermSubjectId === subject.id) ?? []])), [gradesQuery.data]);
  const openAssessmentDialog = () => {
    const firstNumericSubject = gradesQuery.data?.subjects.find((item) => item.assessmentMode === 'NUMERIC');
    const nextOrder = Math.max(0, ...(gradesQuery.data?.assessments.map((item) => item.displayOrder) ?? [])) + 1;
    setAssessment({ ...initialAssessment, studentTermSubjectId: firstNumericSubject?.id ?? '', displayOrder: nextOrder });
    setAssessmentOpen(true);
  };

  return <Stack spacing={2.5}>
    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: { xs: 'flex-start', sm: 'center' }, gap: 2, flexWrap: 'wrap' }}>
      <Box><Typography variant="h4">Điểm số</Typography><Typography color="text.secondary">Quản lý môn học, điểm thường xuyên, giữa kỳ và cuối kỳ.</Typography></Box>
      <Stack direction="row" spacing={1}><Button variant="outlined" startIcon={<AddRoundedIcon />} onClick={() => setSubjectOpen(true)}>Thêm môn</Button><Button variant="contained" startIcon={<AssessmentOutlinedIcon />} onClick={openAssessmentDialog} disabled={!gradesQuery.data?.subjects.some((item) => item.assessmentMode === 'NUMERIC')}>Nhập điểm</Button></Stack>
    </Box>
    {feedback && <Alert severity="success" onClose={() => setFeedback(null)}>{feedback}</Alert>}
    {(subjectMutation.error || assessmentMutation.error) && <Alert severity="error">{errorMessage(subjectMutation.error ?? assessmentMutation.error)}</Alert>}
    <Card sx={{ p: 2 }}><Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
      <TextField select label="Học sinh" value={studentId} onChange={(event) => setStudentId(event.target.value)} sx={{ minWidth: 280 }}>{studentsQuery.data?.items.map((student) => <MenuItem key={student.id} value={student.id}>{student.fullName} · {student.studentCode}</MenuItem>)}</TextField>
      <TextField select label="Học kỳ" value={termId} onChange={(event) => setTermId(event.target.value)} sx={{ minWidth: 220 }}>{catalogQuery.data?.terms.map((term) => <MenuItem key={term.id} value={term.id}>{term.name} · {term.code}</MenuItem>)}</TextField>
    </Stack></Card>
    {gradesQuery.isLoading ? <Box sx={{ py: 8, textAlign: 'center' }}><CircularProgress /></Box> : <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: 'repeat(2, 1fr)' }, gap: 1.5 }}>
      {gradesQuery.data?.subjects.map((subject) => <Card variant="outlined" key={subject.id} sx={{ p: 2 }}>
        <Stack direction="row" sx={{ mb: 1.5, justifyContent: 'space-between', alignItems: 'center' }}><Box><Typography sx={{ fontWeight: 750 }}>{subject.subjectName}</Typography><Typography variant="caption" color="text.secondary">{subject.assessmentMode === 'NUMERIC' ? 'Chấm điểm thang 10' : 'Đạt / Chưa đạt'}</Typography></Box><Chip size="small" label={`${assessmentsBySubject.get(subject.id)?.length ?? 0} cột điểm`} /></Stack>
        <Stack spacing={0.75}>{assessmentsBySubject.get(subject.id)?.map((item) => <Box key={item.id} sx={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: 2, py: 1, borderTop: 1, borderColor: 'divider' }}><Box><Typography variant="body2" sx={{ fontWeight: 650 }}>{item.displayLabel}</Typography><Typography variant="caption" color="text.secondary">{assessmentKindLabels[item.assessmentKind] ?? 'Khác'} · {assessmentFormLabels[item.assessmentForm] ?? 'Khác'}{item.assessedOn ? ` · ${new Intl.DateTimeFormat('vi-VN').format(new Date(`${item.assessedOn}T00:00:00`))}` : ''}</Typography></Box><Typography sx={{ fontWeight: 800, color: item.score == null ? 'text.secondary' : 'primary.main' }}>{formatAssessmentResult(item.score, item.outcome)}</Typography></Box>)}</Stack>
      </Card>)}
      {!gradesQuery.data?.subjects.length && <Card variant="outlined" sx={{ p: 4, textAlign: 'center' }}><Typography color="text.secondary">Học sinh chưa được gán môn trong học kỳ này.</Typography></Card>}
    </Box>}

    <Dialog open={subjectOpen} onClose={() => setSubjectOpen(false)} fullWidth maxWidth="sm"><DialogTitle>Thêm môn học</DialogTitle><DialogContent><Stack spacing={2} sx={{ pt: 1 }}>
      <TextField select label="Môn học" value={subjectForm.subjectId} onChange={(event) => setSubjectForm({ ...subjectForm, subjectId: event.target.value })}>{catalogQuery.data?.subjects.filter((item) => item.enabled && !gradesQuery.data?.subjects.some((existing) => existing.subjectId === item.id)).map((item) => <MenuItem key={item.id} value={item.id}>{item.name}</MenuItem>)}</TextField>
      <TextField select label="Hình thức đánh giá" value={subjectForm.assessmentMode} onChange={(event) => setSubjectForm({ ...subjectForm, assessmentMode: event.target.value })}><MenuItem value="NUMERIC">Điểm thang 10</MenuItem><MenuItem value="REMARK">Đạt / Chưa đạt</MenuItem></TextField>
      {subjectForm.assessmentMode === 'NUMERIC' && <TextField type="number" label="Số tiết cả năm" value={subjectForm.annualLessonCount} onChange={(event) => setSubjectForm({ ...subjectForm, annualLessonCount: Number(event.target.value) })} />}
      <TextField type="number" label="Thứ tự hiển thị" value={subjectForm.displayOrder} onChange={(event) => setSubjectForm({ ...subjectForm, displayOrder: Number(event.target.value) })} />
    </Stack></DialogContent><DialogActions><Button onClick={() => setSubjectOpen(false)}>Hủy</Button><Button variant="contained" disabled={!subjectForm.subjectId || subjectMutation.isPending} onClick={() => subjectMutation.mutate()}>Lưu môn học</Button></DialogActions></Dialog>

    <Dialog open={assessmentOpen} onClose={() => setAssessmentOpen(false)} fullWidth maxWidth="sm"><DialogTitle>Nhập điểm thành phần</DialogTitle><DialogContent><Stack spacing={2} sx={{ pt: 1 }}>
      <TextField select label="Môn học" value={assessment.studentTermSubjectId} onChange={(event) => setAssessment({ ...assessment, studentTermSubjectId: event.target.value })}>{gradesQuery.data?.subjects.filter((item) => item.assessmentMode === 'NUMERIC').map((item) => <MenuItem key={item.id} value={item.id}>{item.subjectName}</MenuItem>)}</TextField>
      <Stack direction="row" spacing={1.5}><TextField select fullWidth label="Loại" value={assessment.assessmentKind} onChange={(event) => setAssessment({ ...assessment, assessmentKind: event.target.value })}><MenuItem value="REGULAR">Thường xuyên</MenuItem><MenuItem value="MIDTERM">Giữa kỳ</MenuItem><MenuItem value="FINAL">Cuối kỳ</MenuItem></TextField><TextField select fullWidth label="Hình thức" value={assessment.assessmentForm} onChange={(event) => setAssessment({ ...assessment, assessmentForm: event.target.value })}><MenuItem value="ORAL">Miệng</MenuItem><MenuItem value="WRITTEN">Viết</MenuItem><MenuItem value="PRESENTATION">Thuyết trình</MenuItem><MenuItem value="PRACTICAL">Thực hành</MenuItem></TextField></Stack>
      <TextField label="Tên cột điểm" value={assessment.displayLabel} onChange={(event) => setAssessment({ ...assessment, displayLabel: event.target.value })} />
      <Stack direction="row" spacing={1.5}><TextField fullWidth type="number" label="Thời lượng (phút)" value={assessment.durationMinutes} onChange={(event) => setAssessment({ ...assessment, durationMinutes: Number(event.target.value) })} /><TextField fullWidth type="number" label="Điểm" value={assessment.score} onChange={(event) => setAssessment({ ...assessment, score: event.target.value })} slotProps={{ htmlInput: { min: 0, max: 10, step: 0.1 } }} /></Stack>
      <TextField type="date" label="Ngày đánh giá" value={assessment.assessedOn} onChange={(event) => setAssessment({ ...assessment, assessedOn: event.target.value })} slotProps={{ inputLabel: { shrink: true } }} />
    </Stack></DialogContent><DialogActions><Button onClick={() => setAssessmentOpen(false)}>Hủy</Button><Button variant="contained" disabled={!assessment.studentTermSubjectId || assessment.score === '' || assessmentMutation.isPending} onClick={() => assessmentMutation.mutate()}>Ghi nhận điểm</Button></DialogActions></Dialog>
  </Stack>;
}
