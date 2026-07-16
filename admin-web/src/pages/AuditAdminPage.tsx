import DownloadRoundedIcon from '@mui/icons-material/DownloadRounded';
import HistoryRoundedIcon from '@mui/icons-material/HistoryRounded';
import SearchRoundedIcon from '@mui/icons-material/SearchRounded';
import {
  Alert, Box, Button, Card, Chip, CircularProgress, InputAdornment, Stack,
  Table, TableBody, TableCell, TableHead, TablePagination, TableRow, TextField,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { downloadAuditCsv, getAudit } from '../api/adminOperationsApi';
import { useAuth } from '../auth/authState';

const actionLabels: Record<string, string> = { CREATE: 'Tạo mới', UPDATE: 'Cập nhật', DELETE: 'Xóa', PUBLISH: 'Xuất bản', UPDATE_STATUS: 'Đổi trạng thái' };

export function AuditAdminPage() {
  const { accessToken } = useAuth(); const [search, setSearch] = useState(''); const [query, setQuery] = useState(''); const [page, setPage] = useState(0); const [size, setSize] = useState(20); const [exportError, setExportError] = useState(false);
  const audit = useQuery({ queryKey:['admin-audit',query,page,size], queryFn:()=>getAudit(accessToken!,query,page,size), enabled:Boolean(accessToken) });
  const exportCsv = async () => { try { setExportError(false); const blob=await downloadAuditCsv(accessToken!,query); const url=URL.createObjectURL(blob); const anchor=document.createElement('a'); anchor.href=url; anchor.download='nhat-ky-quan-tri.csv'; anchor.click(); URL.revokeObjectURL(url); } catch { setExportError(true); } };
  return <Stack spacing={2.5}>
    <Box sx={{display:'flex',justifyContent:'space-between',alignItems:{xs:'flex-start',sm:'center'},gap:2,flexWrap:'wrap'}}><Box><Typography variant="h4">Nhật ký</Typography><Typography color="text.secondary">Theo dõi các thay đổi có đặc quyền trên toàn hệ thống.</Typography></Box><Button variant="outlined" startIcon={<DownloadRoundedIcon/>} onClick={exportCsv}>Xuất CSV</Button></Box>
    {exportError&&<Alert severity="error">Không thể xuất tệp nhật ký.</Alert>}
    <Card sx={{p:2}}><Stack direction={{xs:'column',sm:'row'}} spacing={1.5}><TextField fullWidth placeholder="Tìm hành động, đối tượng hoặc quản trị viên" value={search} onChange={(e)=>setSearch(e.target.value)} onKeyDown={(e)=>{if(e.key==='Enter'){setQuery(search.trim());setPage(0);}}} slotProps={{input:{startAdornment:<InputAdornment position="start"><SearchRoundedIcon/></InputAdornment>}}}/><Button variant="contained" onClick={()=>{setQuery(search.trim());setPage(0);}}>Tìm kiếm</Button></Stack></Card>
    <Card variant="outlined"><Box sx={{px:2,py:1.5,display:'flex',alignItems:'center',gap:1}}><HistoryRoundedIcon color="primary"/><Typography sx={{fontWeight:700}}>Lịch sử thao tác</Typography><Chip size="small" label={`${audit.data?.totalElements??0} bản ghi`}/></Box>{audit.isLoading?<Box sx={{py:8,textAlign:'center'}}><CircularProgress/></Box>:<Table size="small"><TableHead><TableRow><TableCell>Thời gian</TableCell><TableCell>Quản trị viên</TableCell><TableCell>Hành động</TableCell><TableCell>Đối tượng</TableCell><TableCell>Mã bản ghi</TableCell></TableRow></TableHead><TableBody>{audit.data?.items.map(item=><TableRow key={item.id}><TableCell>{new Intl.DateTimeFormat('vi-VN',{dateStyle:'short',timeStyle:'medium'}).format(new Date(item.occurredAt))}</TableCell><TableCell>{item.actorName}</TableCell><TableCell><Chip size="small" color={item.action==='DELETE'?'error':item.action==='CREATE'||item.action==='PUBLISH'?'success':'default'} label={actionLabels[item.action]??item.action}/></TableCell><TableCell>{item.entityType}</TableCell><TableCell><Typography variant="caption" sx={{fontFamily:'monospace'}}>{item.entityId.slice(0,8)}…</Typography></TableCell></TableRow>)}</TableBody></Table>}<TablePagination component="div" count={audit.data?.totalElements??0} page={page} rowsPerPage={size} onPageChange={(_,value)=>setPage(value)} onRowsPerPageChange={(e)=>{setSize(Number(e.target.value));setPage(0);}} rowsPerPageOptions={[10,20,50]} labelRowsPerPage="Số dòng"/></Card>
  </Stack>;
}
