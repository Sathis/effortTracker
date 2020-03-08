package com.ust.effortTracker;

import com.ust.effortTracker.model.EffortTrackerModel;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.LocalDate.parse;
import static java.time.format.DateTimeFormatter.ofPattern;


public class EffortTracker {

    private static final String[] USER_IDS = new String[]{"U11806", "U20931", "U21094", "U24446"};

    private static LocalDate START_DATE = null;

    private static LocalDate END_DATE = null;

    private static final DateTimeFormatter DATE_FORMAT = ofPattern("M/d/yyyy");


    public static void main(String[] args) throws Exception {
        List<EffortTrackerModel> effortTrackerModels = new ArrayList<>();
        Map<String, List<EffortTrackerModel>> entries = new HashMap<>();
        if (checkProgramArgs(args)) {
            Iterator<Row> rowIterator = readEffortTrackerFile(args[0]);
            boolean skipHeader = true;
            while (rowIterator.hasNext()) {
                if (skipHeader) {
                    rowIterator.next();
                    skipHeader = false;
                } else {
                    Row next = rowIterator.next();
                    effortTrackerModels.add(createEffortTrackerModel(next));
                }
            }

            effortTrackerModels.stream().forEach(effortTrackerModel -> {
                groupEffortTrackerModelsByUID(entries, effortTrackerModel);
            });
            Map<String, List<LocalDate>> missedEffort = validateEffortTracking(entries);
            missedEffort.forEach((key, value) -> {

                System.out.println("User: - " + key + " did not enter time for the following date " +
                        value.stream()
                                .map(localDate -> localDate.format(DATE_FORMAT))
                                .collect(Collectors.joining(", ")));
            });
        } else {
            System.out.println("Invalid Program argument");
        }
    }


    private static Map<String, List<LocalDate>> validateEffortTracking(Map<String, List<EffortTrackerModel>> entries) {
        Map<String, List<LocalDate>> missedEntries = new HashMap<>();
        final List<LocalDate> dateRanges = iterateDates(START_DATE, END_DATE);
        Arrays.stream(USER_IDS).forEach(userId -> {
            if (entries.containsKey(userId)) {
                List<EffortTrackerModel> effortTrackerModels = entries.get(userId);
                List<LocalDate> missedDates = new ArrayList<>();

                for (LocalDate date : dateRanges) {
                    boolean match = effortTrackerModels.stream().anyMatch(effortTrackerModel -> {
                        return effortTrackerModel.getDate().isEqual(date);
                    });
                    if (!match) {
                        missedDates.add(date);
                    }
                }
                if (missedDates.size() > 0) {
                    missedEntries.putIfAbsent(userId, missedDates);
                }
            } else {
                missedEntries.putIfAbsent(userId, dateRanges);
            }
        });
        return missedEntries;
    }

    private static List<LocalDate> iterateDates(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dateRanges = new ArrayList<>();
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            dateRanges.add(date);
        }
        dateRanges.add(endDate);
        return dateRanges;
    }

    private static void groupEffortTrackerModelsByUID(Map<String, List<EffortTrackerModel>> entries,
                                                      EffortTrackerModel effortTrackerModel) {
        if (entries.containsKey(effortTrackerModel.getUid())) {
            entries.get(effortTrackerModel.getUid()).add(effortTrackerModel);
        } else {
            entries.put(effortTrackerModel.getUid(), new ArrayList<>(Arrays.asList(effortTrackerModel)));
        }
    }


    private static EffortTrackerModel createEffortTrackerModel(Row next) {
        EffortTrackerModel model = new EffortTrackerModel();
        model.setName(next.getCell(0).getStringCellValue());
        model.setUid(next.getCell(1).getStringCellValue());
        model.setDate(next.getCell(2).getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        model.setProjectName(next.getCell(3).getStringCellValue());
        return model;
    }

    private static Iterator<Row> readEffortTrackerFile(String filePath) throws Exception {
        FileInputStream excelFile = new FileInputStream(new File(filePath));
        Workbook workbook = new XSSFWorkbook(excelFile);
        return workbook.getSheetAt(0).rowIterator();
    }

    private static boolean checkProgramArgs(String[] args) {
        if (args != null && args.length > 0) {
            START_DATE = parse(args[1], DATE_FORMAT);
            END_DATE = parse(args[2], DATE_FORMAT);
            return true;
        }
        return false;
    }
}