import AddRoundedIcon from '@mui/icons-material/AddRounded';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
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
import { z } from 'zod';
import {
  createTeacher,
  getTeachers,
  updateTeacher,
  type AdminTeacher,
} from '../api/adminIdentityApi';
import { ApiProblem } from '../api/adminAuthApi';
import { useAuth } from '../auth/authState';

const teacherSchema = z.object({
  teacherCode: z.string().trim().min(2, 'Nhập mã giáo viên').max(32),
  fullName: z.string().trim().min(2, 'Nhập họ tên giáo viên').max(120),
  email: z.union([z.literal(''), z.string().trim().email('Email không hợp lệ').max(190)]),
  enabled: z.boolean(),
});

type TeacherFormValues = z.infer<typeof teacherSchema>;

const defaultValues: TeacherFormValues = {
  teacherCode: '',
  fullName: '',
  email: '',
  enabled: true,
};

/** Which teachers to show, by whether they can sign in yet. */
type AccountFilter = 'all' | 'with' | 'without';

const ACCOUNT_FILTERS: Record<AccountFilter, boolean | undefined> = {
  all: undefined,
  with: true,
  without: false,
};

function errorMessage(error: unknown): string {
  return error instanceof ApiProblem ? error.message : 'Không thể lưu hồ sơ giáo viên.';
}

export function TeachersPage() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const deferredSearch = useDeferredValue(search);
  const [accountFilter, setAccountFilter] = useState<AccountFilter>('all');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [editing, setEditing] = useState<AdminTeacher | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);

  const filters = {
    page,
    size,
    query: deferredSearch.trim() || undefined,
    hasAccount: ACCOUNT_FILTERS[accountFilter],
  };

  const teachers = useQuery({
    queryKey: ['admin', 'teachers', filters],
    queryFn: () => getTeachers(accessToken ?? '', filters),
    enabled: Boolean(accessToken),
  });

  const form = useForm<TeacherFormValues>({
    resolver: zodResolver(teacherSchema),
    defaultValues,
  });

  useEffect(() => {
    setPage(0);
  }, [deferredSearch, accountFilter]);

  const save = useMutation({
    mutationFn: async (values: TeacherFormValues) => {
      const email = values.email.trim() || undefined;
      if (editing) {
        return updateTeacher(accessToken ?? '', editing.id, {
          teacherCode: values.teacherCode,
          fullName: values.fullName,
          email,
          enabled: values.enabled,
          version: editing.version,
        });
      }
      return createTeacher(accessToken ?? '', {
        teacherCode: values.teacherCode,
        fullName: values.fullName,
        email,
      });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'teachers'] });
      closeDrawer();
    },
  });

  function openCreate() {
    setEditing(null);
    form.reset(defaultValues);
    save.reset();
    setDrawerOpen(true);
  }

  function openEdit(teacher: AdminTeacher) {
    setEditing(teacher);
    form.reset({
      teacherCode: teacher.teacherCode,
      fullName: teacher.fullName,
      email: teacher.email ?? '',
      enabled: teacher.enabled,
    });
    save.reset();
    setDrawerOpen(true);
  }

  function closeDrawer() {
    setDrawerOpen(false);
    setEditing(null);
  }

  const items = teachers.data?.items ?? [];

  return (
    <Stack spacing={3}>
      <Stack direction="row" spacing={2} sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
        <Box>
          <Typography variant="h5" component="h1">
            Giáo viên
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Hồ sơ giáo viên. Giáo viên chưa có tài khoản vẫn được phân công và xếp lịch dạy.
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<AddRoundedIcon />} onClick={openCreate}>
          Thêm giáo viên
        </Button>
      </Stack>

      <Card variant="outlined">
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ p: 2 }}>
          <TextField
            label="Tìm theo tên hoặc mã"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            fullWidth
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchRoundedIcon fontSize="small" />
                  </InputAdornment>
                ),
              },
            }}
          />
          <FormControl sx={{ minWidth: 220 }}>
            <InputLabel id="teacher-account-filter">Tài khoản</InputLabel>
            <Select
              labelId="teacher-account-filter"
              label="Tài khoản"
              value={accountFilter}
              onChange={(event) => setAccountFilter(event.target.value as AccountFilter)}
            >
              <MenuItem value="all">Tất cả</MenuItem>
              <MenuItem value="with">Đã có tài khoản</MenuItem>
              <MenuItem value="without">Chưa có tài khoản</MenuItem>
            </Select>
          </FormControl>
        </Stack>
        <Divider />

        {teachers.isError ? (
          <Alert severity="error" sx={{ m: 2 }}>
            Không tải được danh sách giáo viên.
          </Alert>
        ) : null}

        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Mã</TableCell>
                <TableCell>Họ tên</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Số điện thoại</TableCell>
                <TableCell>Tài khoản</TableCell>
                <TableCell>Trạng thái</TableCell>
                <TableCell align="right">Sửa</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {teachers.isPending
                ? Array.from({ length: 3 }).map((_, index) => (
                    <TableRow key={index}>
                      <TableCell colSpan={7}>
                        <Skeleton height={28} />
                      </TableCell>
                    </TableRow>
                  ))
                : null}

              {!teachers.isPending && items.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7}>
                    <Stack spacing={1} sx={{ alignItems: 'center', py: 4 }}>
                      <SchoolOutlinedIcon color="disabled" />
                      <Typography variant="body2" color="text.secondary">
                        Chưa có giáo viên nào khớp bộ lọc.
                      </Typography>
                    </Stack>
                  </TableCell>
                </TableRow>
              ) : null}

              {items.map((teacher) => (
                <TableRow key={teacher.id} hover>
                  <TableCell>{teacher.teacherCode}</TableCell>
                  <TableCell>{teacher.fullName}</TableCell>
                  <TableCell>{teacher.email ?? '—'}</TableCell>
                  <TableCell>{teacher.phoneNumber ?? '—'}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={teacher.hasAccount ? 'Đã có' : 'Chưa có'}
                      color={teacher.hasAccount ? 'success' : 'default'}
                      variant={teacher.hasAccount ? 'filled' : 'outlined'}
                    />
                  </TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={teacher.enabled ? 'Đang dạy' : 'Ngừng'}
                      color={teacher.enabled ? 'primary' : 'default'}
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell align="right">
                    <IconButton
                      aria-label={`Sửa ${teacher.fullName}`}
                      onClick={() => openEdit(teacher)}
                    >
                      <EditOutlinedIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>

        <TablePagination
          component="div"
          count={teachers.data?.totalElements ?? 0}
          page={page}
          rowsPerPage={size}
          onPageChange={(_, nextPage) => setPage(nextPage)}
          onRowsPerPageChange={(event) => {
            setSize(Number(event.target.value));
            setPage(0);
          }}
          rowsPerPageOptions={[10, 20, 50]}
          labelRowsPerPage="Số dòng"
        />
      </Card>

      <Drawer anchor="right" open={drawerOpen} onClose={closeDrawer}>
        <Box component="form" onSubmit={form.handleSubmit((values) => save.mutate(values))}
             sx={{ width: { xs: '100vw', sm: 420 }, p: 3 }}>
          <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
            <Typography variant="h6">
              {editing ? 'Sửa hồ sơ giáo viên' : 'Thêm giáo viên'}
            </Typography>
            <IconButton aria-label="Đóng" onClick={closeDrawer}>
              <CloseRoundedIcon />
            </IconButton>
          </Stack>

          <Stack spacing={2} sx={{ mt: 2 }}>
            {save.isError ? <Alert severity="error">{errorMessage(save.error)}</Alert> : null}

            <Controller
              name="teacherCode"
              control={form.control}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="Mã giáo viên"
                  error={Boolean(fieldState.error)}
                  helperText={fieldState.error?.message}
                  fullWidth
                />
              )}
            />
            <Controller
              name="fullName"
              control={form.control}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="Họ tên"
                  error={Boolean(fieldState.error)}
                  helperText={fieldState.error?.message}
                  fullWidth
                />
              )}
            />
            <Controller
              name="email"
              control={form.control}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="Email (không bắt buộc)"
                  error={Boolean(fieldState.error)}
                  helperText={fieldState.error?.message}
                  fullWidth
                />
              )}
            />

            {editing ? (
              <Controller
                name="enabled"
                control={form.control}
                render={({ field }) => (
                  <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
                    <Typography variant="body2">Đang dạy</Typography>
                    <Switch checked={field.value} onChange={(event) => field.onChange(event.target.checked)} />
                  </Stack>
                )}
              />
            ) : (
              <Alert severity="info">
                Tài khoản đăng nhập được cấp riêng sau. Giáo viên chưa có tài khoản vẫn được
                phân công và xếp lịch dạy.
              </Alert>
            )}

            <Button
              type="submit"
              variant="contained"
              disabled={save.isPending}
              startIcon={save.isPending ? <CircularProgress size={16} /> : null}
            >
              {editing ? 'Lưu thay đổi' : 'Thêm giáo viên'}
            </Button>
          </Stack>
        </Box>
      </Drawer>
    </Stack>
  );
}
