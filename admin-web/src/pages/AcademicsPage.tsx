import AddRoundedIcon from '@mui/icons-material/AddRounded';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import ClassOutlinedIcon from '@mui/icons-material/ClassOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Grid,
  IconButton,
  MenuItem,
  Skeleton,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import {
  createAcademicItem,
  createSchoolClass,
  deleteAcademicItem,
  getAcademicCatalog,
  updateAcademicItem,
  updateSchoolClass,
  type AcademicSubject,
  type AcademicTerm,
  type AcademicYear,
  type AcademicCatalog,
  type SchoolClass,
  type SchoolClassInput,
} from '../api/adminAcademicsApi';
import { ApiProblem } from '../api/adminAuthApi';
import { useAuth } from '../auth/authState';

type DialogMode = 'year' | 'term' | 'subject' | 'class' | 'edit-year' | 'edit-term' | 'edit-subject' | 'edit-class';

const dialogTitles: Record<DialogMode, string> = {
  year: 'Thêm năm học',
  term: 'Thêm học kỳ',
  subject: 'Thêm môn học',
  class: 'Thêm lớp học',
  'edit-year': 'Cập nhật năm học',
  'edit-term': 'Cập nhật học kỳ',
  'edit-subject': 'Cập nhật môn học',
  'edit-class': 'Cập nhật lớp học',
};

interface DeleteCandidate {
  resource: 'years' | 'terms' | 'subjects' | 'classes';
  id: string;
  version: number;
  label: string;
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('vi-VN').format(new Date(`${value}T00:00:00`));
}

function problemMessage(error: unknown): string {
  return error instanceof ApiProblem ? error.message : 'Không thể lưu dữ liệu học vụ.';
}

export function AcademicsPage() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState(0);
  const [dialogMode, setDialogMode] = useState<DialogMode | null>(null);
  const [editingItem, setEditingItem] = useState<{ id: string; version: number } | null>(null);
  const [editingClass, setEditingClass] = useState<SchoolClass | null>(null);
  const [deleteCandidate, setDeleteCandidate] = useState<DeleteCandidate | null>(null);
  const [form, setForm] = useState({
    academicYearId: '', code: '', name: '', startsOn: '', endsOn: '', gradeLevel: '10', enabled: true,
  });
  const [successMessage, setSuccessMessage] = useState('');

  const catalog = useQuery({
    queryKey: ['admin-academic-catalog'],
    queryFn: () => getAcademicCatalog(accessToken!),
    enabled: Boolean(accessToken),
  });

  const activeYear = useMemo(() => catalog.data?.academicYears[0], [catalog.data]);

  const mutation = useMutation({
    mutationFn: async () => {
      if (!dialogMode) return '';
      if (!form.code.trim()) throw new Error('Vui lòng nhập mã.');
      if (dialogMode === 'year') {
        await createAcademicItem(accessToken!, 'years', { code: form.code, startsOn: form.startsOn, endsOn: form.endsOn });
        return 'Đã tạo năm học.';
      }
      if (dialogMode === 'edit-year' && editingItem) {
        await updateAcademicItem(accessToken!, 'years', editingItem.id, { code: form.code, startsOn: form.startsOn, endsOn: form.endsOn, version: editingItem.version });
        return 'Đã cập nhật năm học.';
      }
      if (dialogMode === 'term') {
        await createAcademicItem(accessToken!, 'terms', { academicYearId: form.academicYearId, code: form.code, name: form.name, startsOn: form.startsOn, endsOn: form.endsOn });
        return 'Đã tạo học kỳ.';
      }
      if (dialogMode === 'edit-term' && editingItem) {
        await updateAcademicItem(accessToken!, 'terms', editingItem.id, { academicYearId: form.academicYearId, code: form.code, name: form.name, startsOn: form.startsOn, endsOn: form.endsOn, version: editingItem.version });
        return 'Đã cập nhật học kỳ.';
      }
      if (dialogMode === 'subject') {
        await createAcademicItem(accessToken!, 'subjects', { code: form.code, name: form.name });
        return 'Đã tạo môn học.';
      }
      if (dialogMode === 'edit-subject' && editingItem) {
        await updateAcademicItem(accessToken!, 'subjects', editingItem.id, { code: form.code, name: form.name, enabled: form.enabled, version: editingItem.version });
        return 'Đã cập nhật môn học.';
      }
      const input: SchoolClassInput = {
        academicYearId: form.academicYearId,
        code: form.code,
        name: form.name,
        gradeLevel: Number(form.gradeLevel),
        enabled: form.enabled,
        version: editingClass?.version ?? 0,
      };
      if (dialogMode === 'edit-class' && editingClass) {
        await updateSchoolClass(accessToken!, editingClass.id, input);
        return 'Đã cập nhật lớp học.';
      }
      await createSchoolClass(accessToken!, input);
      return 'Đã tạo lớp học.';
    },
    onSuccess: async (message) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['admin-academic-catalog'] }),
        queryClient.invalidateQueries({ queryKey: ['admin-dashboard'] }),
      ]);
      setSuccessMessage(message);
      setDialogMode(null);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: async (candidate: DeleteCandidate) => {
      await deleteAcademicItem(accessToken!, candidate.resource, candidate.id, candidate.version);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin-academic-catalog'] });
      setSuccessMessage('Đã xóa dữ liệu học vụ.');
      setDeleteCandidate(null);
    },
  });

  const openDialog = (mode: Exclude<DialogMode, 'edit-class'>) => {
    setEditingItem(null);
    setEditingClass(null);
    setForm({
      academicYearId: activeYear?.id ?? '', code: '', name: '', startsOn: '', endsOn: '', gradeLevel: '10', enabled: true,
    });
    mutation.reset();
    setDialogMode(mode);
  };

  const openEditYear = (year: AcademicYear) => {
    setEditingItem({ id: year.id, version: year.version });
    setEditingClass(null);
    setForm({ academicYearId: '', code: year.code, name: '', startsOn: year.startsOn, endsOn: year.endsOn, gradeLevel: '10', enabled: true });
    mutation.reset();
    setDialogMode('edit-year');
  };

  const openEditTerm = (term: AcademicTerm) => {
    setEditingItem({ id: term.id, version: term.version });
    setEditingClass(null);
    setForm({ academicYearId: term.academicYearId, code: term.code, name: term.name, startsOn: term.startsOn, endsOn: term.endsOn, gradeLevel: '10', enabled: true });
    mutation.reset();
    setDialogMode('edit-term');
  };

  const openEditSubject = (subject: AcademicSubject) => {
    setEditingItem({ id: subject.id, version: subject.version });
    setEditingClass(null);
    setForm({ academicYearId: '', code: subject.code, name: subject.name, startsOn: '', endsOn: '', gradeLevel: '10', enabled: subject.enabled });
    mutation.reset();
    setDialogMode('edit-subject');
  };

  const openEditClass = (schoolClass: SchoolClass) => {
    setEditingItem(null);
    setEditingClass(schoolClass);
    setForm({
      academicYearId: schoolClass.academicYearId,
      code: schoolClass.code,
      name: schoolClass.name,
      startsOn: '',
      endsOn: '',
      gradeLevel: String(schoolClass.gradeLevel),
      enabled: schoolClass.enabled,
    });
    mutation.reset();
    setDialogMode('edit-class');
  };

  if (catalog.isError) {
    return <Box sx={{ maxWidth: 1280, mx: 'auto' }}><Typography component="h1" variant="h1">Học vụ</Typography><Alert severity="error" sx={{ mt: 3 }} action={<Button color="inherit" onClick={() => catalog.refetch()}>Thử lại</Button>}>Không thể tải dữ liệu học vụ.</Alert></Box>;
  }

  return (
    <Box sx={{ maxWidth: 1280, mx: 'auto' }}>
      <Stack direction={{ xs: 'column', lg: 'row' }} sx={{ justifyContent: 'space-between', alignItems: { lg: 'center' }, gap: 2, mb: 2.5 }}>
        <Box><Typography component="h1" variant="h1">Học vụ</Typography><Typography color="text.secondary" sx={{ mt: 0.5 }}>Năm học, học kỳ, môn học và lớp dùng chung cho toàn hệ thống.</Typography></Box>
        <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1 }}>
          <Button variant="outlined" startIcon={<AddRoundedIcon />} onClick={() => openDialog('year')}>Năm học</Button>
          <Button variant="outlined" startIcon={<AddRoundedIcon />} onClick={() => openDialog('term')}>Học kỳ</Button>
          <Button variant="outlined" startIcon={<AddRoundedIcon />} onClick={() => openDialog('subject')}>Môn học</Button>
          <Button variant="contained" startIcon={<AddRoundedIcon />} onClick={() => openDialog('class')}>Lớp học</Button>
        </Stack>
      </Stack>

      <Grid container spacing={2} sx={{ mb: 2 }}>
        {[
          { label: 'Năm học', value: catalog.data?.academicYears.length, icon: <SchoolOutlinedIcon />, color: '#2563eb', background: '#eff6ff' },
          { label: 'Học kỳ', value: catalog.data?.terms.length, icon: <CalendarMonthOutlinedIcon />, color: '#7c3aed', background: '#f5f3ff' },
          { label: 'Môn học', value: catalog.data?.subjects.length, icon: <MenuBookOutlinedIcon />, color: '#d97706', background: '#fff7ed' },
          { label: 'Lớp học', value: catalog.data?.classes.length, icon: <ClassOutlinedIcon />, color: '#059669', background: '#ecfdf5' },
        ].map((item) => <Grid key={item.label} size={{ xs: 6, md: 3 }}><Card><CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}><Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}><Box><Typography color="text.secondary" sx={{ fontSize: 12 }}>{item.label}</Typography>{item.value === undefined ? <Skeleton width={35} height={34} /> : <Typography sx={{ fontSize: 25, fontWeight: 720 }}>{item.value}</Typography>}</Box><Box sx={{ width: 38, height: 38, display: 'grid', placeItems: 'center', borderRadius: 2, color: item.color, bgcolor: item.background }}>{item.icon}</Box></Stack></CardContent></Card></Grid>)}
      </Grid>

      <Card>
        <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ px: 2, minHeight: 52 }}>
          <Tab label="Lớp học" />
          <Tab label="Môn học" />
          <Tab label="Năm học & học kỳ" />
        </Tabs>
        <Divider />
        {tab === 0 && <ClassesTable catalog={catalog.data} loading={catalog.isLoading} onEdit={openEditClass} onDelete={(item) => setDeleteCandidate({ resource: 'classes', id: item.id, version: item.version, label: `lớp ${item.code}` })} />}
        {tab === 1 && <SubjectsTable catalog={catalog.data} loading={catalog.isLoading} onEdit={openEditSubject} onDelete={(item) => setDeleteCandidate({ resource: 'subjects', id: item.id, version: item.version, label: `môn ${item.name}` })} />}
        {tab === 2 && <YearsAndTerms catalog={catalog.data} loading={catalog.isLoading} onEditYear={openEditYear} onEditTerm={openEditTerm} onDelete={setDeleteCandidate} />}
      </Card>

      <Dialog open={Boolean(dialogMode)} onClose={() => !mutation.isPending && setDialogMode(null)} fullWidth maxWidth="sm">
        <DialogTitle>{dialogMode ? dialogTitles[dialogMode] : ''}</DialogTitle>
        <Divider />
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {mutation.isError && <Alert severity="error">{problemMessage(mutation.error)}</Alert>}
            {(dialogMode === 'term' || dialogMode === 'edit-term' || dialogMode === 'class' || dialogMode === 'edit-class') && <TextField select label="Năm học" value={form.academicYearId} onChange={(event) => setForm((current) => ({ ...current, academicYearId: event.target.value }))}>{catalog.data?.academicYears.map((year) => <MenuItem key={year.id} value={year.id}>{year.code}</MenuItem>)}</TextField>}
            <TextField autoFocus label={dialogMode === 'year' || dialogMode === 'edit-year' ? 'Mã năm học' : dialogMode === 'subject' || dialogMode === 'edit-subject' ? 'Mã môn học' : dialogMode === 'term' || dialogMode === 'edit-term' ? 'Mã học kỳ' : 'Mã lớp'} value={form.code} onChange={(event) => setForm((current) => ({ ...current, code: event.target.value.toUpperCase() }))} />
            {dialogMode !== 'year' && dialogMode !== 'edit-year' && <TextField label={dialogMode === 'subject' || dialogMode === 'edit-subject' ? 'Tên môn học' : dialogMode === 'term' || dialogMode === 'edit-term' ? 'Tên học kỳ' : 'Tên lớp'} value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} />}
            {(dialogMode === 'year' || dialogMode === 'edit-year' || dialogMode === 'term' || dialogMode === 'edit-term') && <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}><TextField fullWidth type="date" label="Ngày bắt đầu" value={form.startsOn} onChange={(event) => setForm((current) => ({ ...current, startsOn: event.target.value }))} slotProps={{ inputLabel: { shrink: true } }} /><TextField fullWidth type="date" label="Ngày kết thúc" value={form.endsOn} onChange={(event) => setForm((current) => ({ ...current, endsOn: event.target.value }))} slotProps={{ inputLabel: { shrink: true } }} /></Stack>}
            {(dialogMode === 'class' || dialogMode === 'edit-class') && <TextField select label="Khối" value={form.gradeLevel} onChange={(event) => setForm((current) => ({ ...current, gradeLevel: event.target.value }))}>{[6, 7, 8, 9, 10, 11, 12].map((grade) => <MenuItem key={grade} value={String(grade)}>Khối {grade}</MenuItem>)}</TextField>}
            {dialogMode === 'edit-class' && <TextField select label="Trạng thái" value={String(form.enabled)} onChange={(event) => setForm((current) => ({ ...current, enabled: event.target.value === 'true' }))}><MenuItem value="true">Đang hoạt động</MenuItem><MenuItem value="false">Đã vô hiệu hóa</MenuItem></TextField>}
            {dialogMode === 'edit-subject' && <TextField select label="Trạng thái" value={String(form.enabled)} onChange={(event) => setForm((current) => ({ ...current, enabled: event.target.value === 'true' }))}><MenuItem value="true">Đang sử dụng</MenuItem><MenuItem value="false">Ngừng sử dụng</MenuItem></TextField>}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}><Button color="inherit" onClick={() => setDialogMode(null)}>Hủy</Button><Button variant="contained" disabled={mutation.isPending} onClick={() => mutation.mutate()} startIcon={mutation.isPending ? <CircularProgress size={16} color="inherit" /> : undefined}>Lưu dữ liệu</Button></DialogActions>
      </Dialog>

      <Dialog open={Boolean(deleteCandidate)} onClose={() => !deleteMutation.isPending && setDeleteCandidate(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Xác nhận xóa</DialogTitle>
        <DialogContent><Stack spacing={2} sx={{ pt: 1 }}>{deleteMutation.isError && <Alert severity="error">{problemMessage(deleteMutation.error)}</Alert>}<Typography>Bạn có chắc muốn xóa {deleteCandidate?.label}? Dữ liệu đang được sử dụng sẽ được hệ thống bảo vệ.</Typography></Stack></DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}><Button color="inherit" onClick={() => setDeleteCandidate(null)}>Hủy</Button><Button color="error" variant="contained" disabled={!deleteCandidate || deleteMutation.isPending} onClick={() => deleteCandidate && deleteMutation.mutate(deleteCandidate)}>Xóa dữ liệu</Button></DialogActions>
      </Dialog>

      {successMessage && <Alert severity="success" sx={{ position: 'fixed', right: 24, top: 88, zIndex: 1400, pointerEvents: 'none' }}>{successMessage}</Alert>}
    </Box>
  );
}

function ClassesTable({ catalog, loading, onEdit, onDelete }: { catalog?: AcademicCatalog; loading: boolean; onEdit: (value: SchoolClass) => void; onDelete: (value: SchoolClass) => void }) {
  return <TableContainer><Table size="small" aria-label="Danh sách lớp học"><TableHead><TableRow><TableCell>Lớp</TableCell><TableCell>Khối</TableCell><TableCell>Năm học</TableCell><TableCell>Sĩ số</TableCell><TableCell>Trạng thái</TableCell><TableCell align="right">Thao tác</TableCell></TableRow></TableHead><TableBody>{loading ? Array.from({ length: 4 }, (_, index) => <TableRow key={index}>{Array.from({ length: 6 }, (__, cell) => <TableCell key={cell}><Skeleton /></TableCell>)}</TableRow>) : catalog?.classes.map((item) => <TableRow hover key={item.id}><TableCell><Typography variant="body2" sx={{ fontWeight: 650 }}>{item.code}</Typography><Typography variant="caption" color="text.secondary">{item.name}</Typography></TableCell><TableCell>Khối {item.gradeLevel}</TableCell><TableCell>{catalog.academicYears.find((year) => year.id === item.academicYearId)?.code ?? '—'}</TableCell><TableCell>{item.studentCount}</TableCell><TableCell><Chip size="small" label={item.enabled ? 'Hoạt động' : 'Vô hiệu hóa'} color={item.enabled ? 'success' : 'default'} /></TableCell><TableCell align="right"><IconButton aria-label={`Sửa lớp ${item.code}`} size="small" onClick={() => onEdit(item)}><EditOutlinedIcon fontSize="small" /></IconButton><IconButton aria-label={`Xóa lớp ${item.code}`} size="small" color="error" onClick={() => onDelete(item)}><DeleteOutlineRoundedIcon fontSize="small" /></IconButton></TableCell></TableRow>)}</TableBody></Table></TableContainer>;
}

function SubjectsTable({ catalog, loading, onEdit, onDelete }: { catalog?: AcademicCatalog; loading: boolean; onEdit: (value: AcademicSubject) => void; onDelete: (value: AcademicSubject) => void }) {
  return <TableContainer><Table size="small" aria-label="Danh sách môn học"><TableHead><TableRow><TableCell>Mã môn</TableCell><TableCell>Tên môn học</TableCell><TableCell>Trạng thái</TableCell><TableCell align="right">Thao tác</TableCell></TableRow></TableHead><TableBody>{loading ? <TableRow><TableCell colSpan={4}><Skeleton /></TableCell></TableRow> : catalog?.subjects.map((item) => <TableRow key={item.id}><TableCell><Chip label={item.code} size="small" variant="outlined" /></TableCell><TableCell><Typography variant="body2" sx={{ fontWeight: 600 }}>{item.name}</Typography></TableCell><TableCell><Chip size="small" label={item.enabled ? 'Đang sử dụng' : 'Ngừng sử dụng'} color={item.enabled ? 'success' : 'default'} /></TableCell><TableCell align="right"><IconButton aria-label={`Sửa môn ${item.name}`} size="small" onClick={() => onEdit(item)}><EditOutlinedIcon fontSize="small" /></IconButton><IconButton aria-label={`Xóa môn ${item.name}`} size="small" color="error" onClick={() => onDelete(item)}><DeleteOutlineRoundedIcon fontSize="small" /></IconButton></TableCell></TableRow>)}</TableBody></Table></TableContainer>;
}

function YearsAndTerms({ catalog, loading, onEditYear, onEditTerm, onDelete }: { catalog?: AcademicCatalog; loading: boolean; onEditYear: (value: AcademicYear) => void; onEditTerm: (value: AcademicTerm) => void; onDelete: (value: DeleteCandidate) => void }) {
  if (loading) return <Box sx={{ p: 3 }}><Skeleton height={90} /></Box>;
  return <Stack divider={<Divider />}>
    {catalog?.academicYears.map((year) => <Box key={year.id} sx={{ p: 2.5 }}><Stack direction={{ xs: 'column', sm: 'row' }} sx={{ justifyContent: 'space-between', gap: 1 }}><Box><Stack direction="row" sx={{ alignItems: 'center', gap: 0.5 }}><Typography variant="body1" sx={{ fontWeight: 680 }}>{year.code}</Typography><IconButton aria-label={`Sửa năm học ${year.code}`} size="small" onClick={() => onEditYear(year)}><EditOutlinedIcon sx={{ fontSize: 17 }} /></IconButton><IconButton aria-label={`Xóa năm học ${year.code}`} size="small" color="error" onClick={() => onDelete({ resource: 'years', id: year.id, version: year.version, label: `năm học ${year.code}` })}><DeleteOutlineRoundedIcon sx={{ fontSize: 17 }} /></IconButton></Stack><Typography color="text.secondary" sx={{ fontSize: 12 }}>{formatDate(year.startsOn)} – {formatDate(year.endsOn)}</Typography></Box><Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1 }}>{catalog.terms.filter((term) => term.academicYearId === year.id).map((term) => <Stack key={term.id} direction="row" sx={{ alignItems: 'center', border: 1, borderColor: 'divider', borderRadius: 5, pl: 1.5 }}><Typography variant="caption">{term.name} · {formatDate(term.startsOn)} – {formatDate(term.endsOn)}</Typography><IconButton aria-label={`Sửa học kỳ ${term.name}`} size="small" onClick={() => onEditTerm(term)}><EditOutlinedIcon sx={{ fontSize: 16 }} /></IconButton><IconButton aria-label={`Xóa học kỳ ${term.name}`} size="small" color="error" onClick={() => onDelete({ resource: 'terms', id: term.id, version: term.version, label: `học kỳ ${term.name}` })}><DeleteOutlineRoundedIcon sx={{ fontSize: 16 }} /></IconButton></Stack>)}</Stack></Stack></Box>)}
  </Stack>;
}
