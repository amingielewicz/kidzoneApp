import {
  Box,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Tooltip,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import CancelIcon from '@mui/icons-material/Cancel';
import VisibilityIcon from '@mui/icons-material/Visibility';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import TableSortLabel from '@mui/material/TableSortLabel';
import { PlaceReport, PLACE_REPORT_REASON_LABELS, PlaceReportReason } from '../../types';
import { statusChip, formatDate, copyToClipboard } from './reportUtils';

interface PlaceReportsTableProps {
  reports: PlaceReport[];
  placeNames: Record<string, string>;
  sortDir: 'asc' | 'desc';
  onToggleSort: () => void;
  onViewDetail: (report: PlaceReport) => void;
  onDelete: (report: PlaceReport) => void;
  onDismiss: (report: PlaceReport) => void;
}

export function PlaceReportsTable({
  reports,
  placeNames,
  sortDir,
  onToggleSort,
  onViewDetail,
  onDelete,
  onDismiss,
}: PlaceReportsTableProps) {
  return (
    <TableContainer component={Paper}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell sx={{ width: 100 }}>
              <TableSortLabel active direction={sortDir} onClick={onToggleSort}>
                Data
              </TableSortLabel>
            </TableCell>
            <TableCell sx={{ width: 150 }}>Nazwa miejsca</TableCell>
            <TableCell>Place ID</TableCell>
            <TableCell sx={{ width: 160 }}>Powód</TableCell>
            <TableCell>Komentarz</TableCell>
            <TableCell sx={{ width: 110 }}>Status</TableCell>
            <TableCell sx={{ width: 100 }}>Akcje</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {reports.map((report) => (
            <TableRow key={report.id} hover>
              <TableCell>{formatDate(report.createdAtMillis)}</TableCell>
              <TableCell>
                <Typography variant="body2" fontWeight={500}>
                  {placeNames[report.placeId] || '\u2014'}
                </Typography>
              </TableCell>
              <TableCell>
                <Box display="flex" alignItems="center" gap={0.5}>
                  <Tooltip title={report.placeId}>
                    <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: 11 }}>
                      {report.placeId.slice(0, 12)}...
                    </Typography>
                  </Tooltip>
                  <Tooltip title="Kopiuj ID">
                    <IconButton size="small" onClick={() => copyToClipboard(report.placeId)}>
                      <ContentCopyIcon sx={{ fontSize: 14 }} />
                    </IconButton>
                  </Tooltip>
                </Box>
              </TableCell>
              <TableCell>
                {PLACE_REPORT_REASON_LABELS[report.reason as PlaceReportReason] || report.reason}
              </TableCell>
              <TableCell sx={{ maxWidth: 200 }}>
                <Tooltip title={report.comment || ''}>
                  <Typography variant="body2" noWrap>
                    {report.comment || '\u2014'}
                  </Typography>
                </Tooltip>
              </TableCell>
              <TableCell>{statusChip(report.status)}</TableCell>
              <TableCell>
                <Box display="flex" flexDirection="row" alignItems="flex-start">
                  <Tooltip title="Szczegóły">
                    <IconButton size="small" onClick={() => onViewDetail(report)}>
                      <VisibilityIcon />
                    </IconButton>
                  </Tooltip>
                  {report.status === 'pending' && (
                    <Box display="flex" flexDirection="column">
                      <Tooltip title="Usun miejsce">
                        <IconButton color="error" size="small" onClick={() => onDelete(report)}>
                          <DeleteIcon />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Odrzuc">
                        <IconButton
                          size="small"
                          sx={{ color: '#1976D2' }}
                          onClick={() => onDismiss(report)}
                        >
                          <CancelIcon />
                        </IconButton>
                      </Tooltip>
                    </Box>
                  )}
                </Box>
              </TableCell>
            </TableRow>
          ))}
          {reports.length === 0 && (
            <TableRow>
              <TableCell colSpan={7} align="center">
                Brak zgloszen
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
