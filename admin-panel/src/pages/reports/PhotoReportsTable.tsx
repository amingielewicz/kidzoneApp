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
  Chip,
  IconButton,
  Tooltip,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import CancelIcon from '@mui/icons-material/Cancel';
import VisibilityIcon from '@mui/icons-material/Visibility';
import TableSortLabel from '@mui/material/TableSortLabel';
import { PhotoReport, PHOTO_REPORT_REASON_LABELS, PhotoReportReason } from '../../types';
import { statusChip, formatDate } from './reportUtils';

interface PhotoReportsTableProps {
  reports: PhotoReport[];
  sortDir: 'asc' | 'desc';
  onToggleSort: () => void;
  onViewDetail: (report: PhotoReport) => void;
  onDelete: (report: PhotoReport) => void;
  onDismiss: (report: PhotoReport) => void;
}

export function PhotoReportsTable({
  reports,
  sortDir,
  onToggleSort,
  onViewDetail,
  onDelete,
  onDismiss,
}: PhotoReportsTableProps) {
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
            <TableCell sx={{ width: 100 }}>Zdjecie</TableCell>
            <TableCell sx={{ width: 160 }}>Powód</TableCell>
            <TableCell>Komentarz</TableCell>
            <TableCell sx={{ width: 110 }}>Status</TableCell>
            <TableCell sx={{ width: 100 }}>Akcje</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {reports.map((report) => {
            const photoMissing = !report.photoUrl;
            const isNotPending = report.status !== 'pending';
            return (
              <TableRow
                key={report.id}
                hover
                sx={isNotPending || photoMissing ? { opacity: 0.6 } : undefined}
              >
                <TableCell>{formatDate(report.createdAtMillis)}</TableCell>
                <TableCell>
                  {report.photoUrl ? (
                    <a href={report.photoUrl} target="_blank" rel="noopener noreferrer">
                      <img
                        src={report.photoUrl}
                        alt="Zdjecie"
                        style={{ width: 60, height: 60, objectFit: 'cover', borderRadius: 4 }}
                      />
                    </a>
                  ) : (
                    <Chip label="Usuniete" size="small" color="default" />
                  )}
                </TableCell>
                <TableCell>
                  {PHOTO_REPORT_REASON_LABELS[report.reason as PhotoReportReason] || report.reason}
                </TableCell>
                <TableCell>
                  <Tooltip title={report.comment || ''}>
                    <Typography variant="body2" noWrap>
                      {report.comment || '\u2014'}
                    </Typography>
                  </Tooltip>
                </TableCell>
                <TableCell>
                  {photoMissing && report.status === 'pending' ? (
                    <Chip label="Nieaktualne" size="small" color="default" />
                  ) : (
                    statusChip(report.status)
                  )}
                </TableCell>
                <TableCell>
                  <Box display="flex" flexDirection="row" alignItems="flex-start">
                    <Tooltip title="Szczegóły">
                      <IconButton
                        size="small"
                        aria-label={`Pokaż szczegóły zgłoszenia zdjęcia ${report.id}`}
                        onClick={() => onViewDetail(report)}
                      >
                        <VisibilityIcon />
                      </IconButton>
                    </Tooltip>
                    {report.status === 'pending' && (
                      <Box display="flex" flexDirection="column">
                        <Tooltip title="Usun zdjecie">
                          <IconButton
                            color="error"
                            size="small"
                            aria-label={`Usuń zdjęcie ze zgłoszenia ${report.id}`}
                            disabled={photoMissing}
                            onClick={() => onDelete(report)}
                          >
                            <DeleteIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Odrzuc">
                          <IconButton
                            size="small"
                            aria-label={`Odrzuć zgłoszenie zdjęcia ${report.id}`}
                            sx={{ color: photoMissing ? undefined : '#1976D2' }}
                            disabled={photoMissing}
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
            );
          })}
          {reports.length === 0 && (
            <TableRow>
              <TableCell colSpan={6} align="center">
                Brak zgloszen
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
