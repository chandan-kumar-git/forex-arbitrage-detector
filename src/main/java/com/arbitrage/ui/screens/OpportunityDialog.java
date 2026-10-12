package com.arbitrage.ui.screens;

import com.arbitrage.model.Opportunity;
import com.arbitrage.model.OpportunityLeg;
import com.arbitrage.ui.theme.AppButton;
import com.arbitrage.ui.theme.Formats;
import com.arbitrage.ui.theme.StatusChip;
import com.arbitrage.ui.theme.Tables;
import com.arbitrage.ui.theme.Theme;
import com.arbitrage.ui.theme.Ui;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.List;

/**
 * A small window with the details of one saved opportunity:
 * the path, the profit, and one table row for every conversion step.
 */
public class OpportunityDialog extends JDialog {

    public OpportunityDialog(Window owner, Opportunity opportunity, List<OpportunityLeg> legs) {
        super(owner, "Opportunity details", ModalityType.APPLICATION_MODAL);
        JPanel content = new JPanel(new BorderLayout(0, Theme.S4));
        content.setBackground(Theme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(Theme.S5, Theme.S5, Theme.S5, Theme.S5));
        content.add(buildHeader(opportunity), BorderLayout.NORTH);
        content.add(Tables.scroll(buildLegsTable(legs)), BorderLayout.CENTER);

        AppButton closeButton = new AppButton("Close", AppButton.Variant.PRIMARY);
        closeButton.addActionListener(e -> dispose());
        content.add(Ui.row(FlowLayout.RIGHT, closeButton), BorderLayout.SOUTH);

        setContentPane(content);
        setSize(520, 360);
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(closeButton);   // Enter closes the dialog
    }

    private JPanel buildHeader(Opportunity opportunity) {
        JLabel pathLabel = new JLabel(opportunity.pathText());
        pathLabel.setFont(Theme.CARD_TITLE);
        pathLabel.setForeground(Theme.INK);
        JLabel factsLabel = new JLabel("Detected " + Formats.dateTime(opportunity.detectedAt())
                + " · fee " + Formats.profit(opportunity.fee() * 100) + " · source " + opportunity.source());
        factsLabel.setFont(Theme.SMALL);
        factsLabel.setForeground(Theme.INK_2);
        StatusChip profitChip = new StatusChip(StatusChip.Kind.GOOD,
                "+" + Formats.profit(opportunity.profitPercent()));

        JPanel header = new JPanel(new BorderLayout(Theme.S3, Theme.S1));
        header.setOpaque(false);
        header.add(pathLabel, BorderLayout.CENTER);
        header.add(Ui.row(FlowLayout.RIGHT, profitChip), BorderLayout.EAST);
        header.add(factsLabel, BorderLayout.SOUTH);
        return header;
    }

    private JTable buildLegsTable(List<OpportunityLeg> legs) {
        DefaultTableModel model = Tables.readOnlyModel("Leg", "From", "To", "Rate");
        for (OpportunityLeg leg : legs) {
            model.addRow(new Object[] {leg.legNo(), leg.fromCode(), leg.toCode(), Formats.price(leg.rate())});
        }
        JTable table = new JTable(model);
        Tables.style(table);
        table.getColumnModel().getColumn(3).setCellRenderer(Tables.monoRenderer());
        return table;
    }
}
