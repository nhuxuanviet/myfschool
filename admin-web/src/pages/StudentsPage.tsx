import AddRoundedIcon from '@mui/icons-material/AddRounded';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import FilterAltOffOutlinedIcon from '@mui/icons-material/FilterAltOffOutlined';
import PeopleAltOutlinedIcon from '@mui/icons-material/PeopleAltOutlined';
import SearchRoundedIcon from '@mui/icons-material/SearchRounded';
import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Divider,
  Drawer,
  FormControl,
  IconButton,
  InputAdornment,
  InputLabel,
  MenuItem,
  Select,
  Skeleton,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useDeferredValue, useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { useSearchParams } from 'react-router-dom';
import { z } from 'zod';
import {
  createStudent,
  getAcademicCatalog,
  getStudents,
  updateStudent,
  type AdminStudent,
  type StudentInput,
} from '../api/adminAcademicsApi';
import { ApiProblem } from '../api/adminAuthApi';
import { useAuth } from '../auth/authState';

const studentSchema = z.object({
  fullName: z.string().trim().min(2, 'Nhập họ tên học sinh').max(120),
  studentCode: z.string().trim().min(2, 'Nhập mã học sinh').max(32),
  phoneNumber: z.string().trim().regex(/^0[35789][0-9]{8}$/, 'Số điện thoại Việt Nam không hợp lệ'),
  classId: z.string().uuid('Chọn lớp học'),
  initialPassword: z.string().max(72),
  enabled: z.boolean(),
});

type StudentFormValues = z.infer<typeof studentSchema>;

const defaultValues: StudentFormValues = {
  fullName: '',
  studentCode: '',
  phoneNumber: '',
  classId: '',
  initialPassword: 'Student@123',
  enabled: true,
};

function errorMessage(error: unknown): string {
  return error instanceof ApiProblem ? error.message : 'Không thể lưu hồ sơ học sinh.';
}

export function StudentsPage() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const [query, setQuery] = useState(searchParams.get('search') ?? '');
  const deferredQuery = useDeferredValue(query.trim());
  const [gradeLevel, setGradeLevel] = useState<number | ''>(() => {
    const value = Number(searchParams.get('grade'));
    return value >= 6 && value <= 12 ? value : '';
  });
  const [classId, setClassId] = useState(searchParams.get('classId') ?? '');
  const [status, setStatus] = useState<'all' | 'active' | 'disabled'>(() => {
    const value = searchParams.get('status');
    return value === 'active' || value === 'disabled' ? value : 'all';
  });
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [editingStudent, setEditingStudent] = useState<AdminStudent | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');

  const catalog = useQuery({
    queryKey: ['admin-academic-catalog'],
    queryFn: () => getAcademicCatalog(accessToken!),
    enabled: Boolean(accessToken),
  });
  const students = useQuery({
    queryKey: ['admin-students', deferredQuery, gradeLevel, classId, status, page, size],
    queryFn: () => getStudents(accessToken!, {
      query: deferredQuery || undefined,
      gradeLevel: gradeLevel || undefined,
      classId: classId || undefined,
      enabled: status === 'all' ? undefined : status === 'active',
      page,
      size,
    }),
    enabled: Boolean(accessToken),
  });

  useEffect(() => setPage(0), [deferredQuery, gradeLevel, classId, status]);
  useEffect(() => {
    const next = new URLSearchParams();
    if (deferredQuery) next.set('search', deferredQuery);
    if (gradeLevel) next.set('grade', String(gradeLevel));
    if (classId) next.set('classId', classId);
    if (status !== 'all') next.set('status', status);
    setSearchParams(next, { replace: true });
  }, [classId, deferredQuery, gradeLevel, setSearchParams, status]);

  const form = useForm<StudentFormValues>({
    resolver: zodResolver(studentSchema),
    defaultValues,
  });

  const mutation = useMutation({
    mutationFn: async (values: StudentFormValues) => {
      if (!editingStudent && values.initialPassword.length < 8) {
        form.setError('initialPassword', { message: 'Mật khẩu ban đầu phải có ít nhất 8 ký tự' });
        throw new Error('invalid initial password');
      }
      const input: StudentInput = {
        ...values,
        studentCode: values.studentCode.toUpperCase(),
        version: editingStudent?.version ?? 0,
        initialPassword: editingStudent ? undefined : values.initialPassword,
      };
      if (editingStudent) {
        await updateStudent(accessToken!, editingStudent.id, input);
        return 'Đã cập nhật hồ sơ học sinh.';
      }
      await createStudent(accessToken!, input);
      return 'Đã tạo tài khoản học sinh.';
    },
    onSuccess: async (message) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['admin-students'] }),
        queryClient.invalidateQueries({ queryKey: ['admin-academic-catalog'] }),
        queryClient.invalidateQueries({ queryKey: ['admin-dashboard'] }),
      ]);
      setSuccessMessage(message);
      setDrawerOpen(false);
    },
  });

  const openCreate = () => {
    setEditingStudent(null);
    form.reset(defaultValues);
    mutation.reset();
    setDrawerOpen(true);
  };

  const openEdit = (student: AdminStudent) => {
    setEditingStudent(student);
    form.reset({
      fullName: student.fullName,
      studentCode: student.studentCode,
      phoneNumber: student.phoneNumber,
      classId: student.classId ?? '',
      initialPassword: '',
      enabled: student.enabled,
    });
    mutation.reset();
    setDrawerOpen(true);
  };

  const clearFilters = () => {
    setQuery('');
    setGradeLevel('');
    setClassId('');
    setStatus('all');
  };

  return (
    <Box sx={{ maxWidth: 1280, mx: 'auto' }}>
      <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ justifyContent: 'space-between', alignItems: { sm: 'center' }, gap: 2, mb: 2.5 }}>
        <Box>
          <Typography component="h1" variant="h1">Học sinh</Typography>
          <Typography color="text.secondary" sx={{ mt: 0.5 }}>Tra cứu và quản lý hồ sơ, lớp học, trạng thái tài khoản.</Typography>
        </Box>
        <Button variant="contained" startIcon={<AddRoundedIcon />} onClick={openCreate}>Thêm học sinh</Button>
      </Stack>

      <Card sx={{ mb: 2 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} sx={{ p: 2 }}>
          <TextField
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Tên, mã học sinh hoặc số điện thoại"
            sx={{ flex: 1, minWidth: 240 }}
            slotProps={{
              input: { startAdornment: <InputAdornment position="start"><SearchRoundedIcon fontSize="small" /></InputAdornment> },
              htmlInput: { 'aria-label': 'Tìm trong danh sách học sinh' },
            }}
          />
          <FormControl size="small" sx={{ minWidth: 130 }}>
            <InputLabel>Khối</InputLabel>
            <Select value={gradeLevel} label="Khối" onChange={(event) => setGradeLevel(event.target.value as number | '')}>
              <MenuItem value="">Tất cả</MenuItem>
              {[6, 7, 8, 9, 10, 11, 12].map((grade) => <MenuItem key={grade} value={grade}>Khối {grade}</MenuItem>)}
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ minWidth: 140 }}>
            <InputLabel>Lớp</InputLabel>
            <Select value={classId} label="Lớp" onChange={(event) => setClassId(event.target.value)}>
              <MenuItem value="">Tất cả</MenuItem>
              {catalog.data?.classes.filter((item) => item.enabled).map((item) => <MenuItem key={item.id} value={item.id}>{item.code}</MenuItem>)}
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ minWidth: 150 }}>
            <InputLabel>Trạng thái</InputLabel>
            <Select value={status} label="Trạng thái" onChange={(event) => setStatus(event.target.value as typeof status)}>
              <MenuItem value="all">Tất cả</MenuItem>
              <MenuItem value="active">Đang hoạt động</MenuItem>
              <MenuItem value="disabled">Đã vô hiệu hóa</MenuItem>
            </Select>
          </FormControl>
          <Button color="inherit" startIcon={<FilterAltOffOutlinedIcon />} onClick={clearFilters}>Xóa lọc</Button>
        </Stack>
      </Card>

      {students.isError ? (
        <Alert severity="error" action={<Button color="inherit" onClick={() => students.refetch()}>Thử lại</Button>}>
          Không thể tải danh sách học sinh.
        </Alert>
      ) : (
        <Card>
          <Box sx={{ px: 2.25, py: 1.75, display: 'flex', alignItems: 'center', gap: 1.25 }}>
            <PeopleAltOutlinedIcon color="primary" />
            <Box>
              <Typography component="h2" variant="h2">Danh sách học sinh</Typography>
              <Typography color="text.secondary" sx={{ fontSize: 12 }}>Có {students.data?.totalElements ?? 0} hồ sơ phù hợp</Typography>
            </Box>
          </Box>
          <Divider />
          <TableContainer>
            <Table aria-label="Danh sách học sinh" size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Học sinh</TableCell>
                  <TableCell>Mã học sinh</TableCell>
                  <TableCell>Lớp</TableCell>
                  <TableCell>Số điện thoại</TableCell>
                  <TableCell>Trạng thái</TableCell>
                  <TableCell align="right">Thao tác</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {students.isLoading ? Array.from({ length: 5 }, (_, index) => (
                  <TableRow key={index}>{Array.from({ length: 6 }, (__, cell) => <TableCell key={cell}><Skeleton /></TableCell>)}</TableRow>
                )) : students.data?.items.length ? students.data.items.map((student) => (
                  <TableRow key={student.id} hover>
                    <TableCell><Typography variant="body2" sx={{ fontWeight: 650 }}>{student.fullName}</Typography><Typography variant="caption" color="text.secondary">Khối {student.gradeLevel}</Typography></TableCell>
                    <TableCell><Typography variant="body2">{student.studentCode}</Typography></TableCell>
                    <TableCell><Chip label={student.classCode} size="small" variant="outlined" /></TableCell>
                    <TableCell>{student.phoneNumber}</TableCell>
                    <TableCell><Chip label={student.enabled ? 'Hoạt động' : 'Vô hiệu hóa'} size="small" color={student.enabled ? 'success' : 'default'} /></TableCell>
                    <TableCell align="right"><IconButton aria-label={`Sửa ${student.fullName}`} size="small" onClick={() => openEdit(student)}><EditOutlinedIcon fontSize="small" /></IconButton></TableCell>
                  </TableRow>
                )) : (
                  <TableRow><TableCell colSpan={6} sx={{ py: 7, textAlign: 'center' }}><Typography color="text.secondary">Không tìm thấy học sinh phù hợp.</Typography></TableCell></TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
          <TablePagination
            component="div"
            count={students.data?.totalElements ?? 0}
            page={page}
            rowsPerPage={size}
            onPageChange={(_, value) => setPage(value)}
            onRowsPerPageChange={(event) => { setSize(Number(event.target.value)); setPage(0); }}
            rowsPerPageOptions={[10, 20, 50]}
            labelRowsPerPage="Số dòng"
          />
        </Card>
      )}

      <Drawer anchor="right" open={drawerOpen} onClose={() => !mutation.isPending && setDrawerOpen(false)} slotProps={{ paper: { sx: { width: { xs: '100%', sm: 460 } } } }}>
        <Stack sx={{ height: '100%' }}>
          <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', px: 3, py: 2.25 }}>
            <Box><Typography component="h2" variant="h2">{editingStudent ? 'Cập nhật học sinh' : 'Thêm học sinh'}</Typography><Typography color="text.secondary" sx={{ fontSize: 12, mt: 0.25 }}>Thông tin được dùng trực tiếp trên ứng dụng học sinh.</Typography></Box>
            <IconButton aria-label="Đóng biểu mẫu" onClick={() => setDrawerOpen(false)}><CloseRoundedIcon /></IconButton>
          </Stack>
          <Divider />
          <Box component="form" onSubmit={form.handleSubmit((values) => mutation.mutate(values))} sx={{ p: 3, overflowY: 'auto', flex: 1 }}>
            <Stack spacing={2}>
              {mutation.isError && !(mutation.error instanceof Error && mutation.error.message === 'invalid initial password') && <Alert severity="error">{errorMessage(mutation.error)}</Alert>}
              <TextField label="Họ và tên" autoFocus {...form.register('fullName')} error={Boolean(form.formState.errors.fullName)} helperText={form.formState.errors.fullName?.message} />
              <TextField label="Mã học sinh" {...form.register('studentCode')} error={Boolean(form.formState.errors.studentCode)} helperText={form.formState.errors.studentCode?.message} />
              <TextField label="Số điện thoại" {...form.register('phoneNumber')} error={Boolean(form.formState.errors.phoneNumber)} helperText={form.formState.errors.phoneNumber?.message} />
              {!editingStudent && <TextField label="Mật khẩu ban đầu" type="password" {...form.register('initialPassword')} error={Boolean(form.formState.errors.initialPassword)} helperText={form.formState.errors.initialPassword?.message ?? 'Tối thiểu 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.'} />}
              <TextField select label="Lớp học" defaultValue="" {...form.register('classId')} error={Boolean(form.formState.errors.classId)} helperText={form.formState.errors.classId?.message}>
                {catalog.data?.classes.filter((item) => item.enabled).map((item) => <MenuItem key={item.id} value={item.id}>{item.code} · Khối {item.gradeLevel}</MenuItem>)}
              </TextField>
              {editingStudent && <Controller name="enabled" control={form.control} render={({ field }) => <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', border: 1, borderColor: 'divider', borderRadius: 2, px: 1.75, py: 1 }}><Box><Typography variant="body2" sx={{ fontWeight: 650 }}>Tài khoản hoạt động</Typography><Typography variant="caption" color="text.secondary">Tắt để chặn học sinh đăng nhập.</Typography></Box><Switch checked={field.value} onChange={field.onChange} /></Stack>} />}
            </Stack>
          </Box>
          <Divider />
          <Stack direction="row" spacing={1.5} sx={{ p: 2.5, justifyContent: 'flex-end' }}>
            <Button color="inherit" onClick={() => setDrawerOpen(false)}>Hủy</Button>
            <Button variant="contained" disabled={mutation.isPending} onClick={form.handleSubmit((values) => mutation.mutate(values))} startIcon={mutation.isPending ? <CircularProgress size={16} color="inherit" /> : undefined}>{editingStudent ? 'Lưu thay đổi' : 'Tạo học sinh'}</Button>
          </Stack>
        </Stack>
      </Drawer>

      {successMessage && <Alert severity="success" sx={{ position: 'fixed', right: 24, top: 88, zIndex: 1400, pointerEvents: 'none' }}>{successMessage}</Alert>}
    </Box>
  );
}
