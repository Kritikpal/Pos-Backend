package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.inventory.entity.unit.UnitMaster;
import com.kritik.POS.inventory.repository.UnitMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnitMasterSeederService {

    private final UnitMasterRepository unitMasterRepository;

    private static final List<UnitSeed> DEFAULT_UNITS = List.of(
            new UnitSeed("UNIT", "Unit"),
            new UnitSeed("PCS", "Pieces"),
            new UnitSeed("PC", "Piece"),
            new UnitSeed("NOS", "Numbers"),
            new UnitSeed("PAIR", "Pair"),
            new UnitSeed("SET", "Set"),
            new UnitSeed("DOZEN", "Dozen"),
            new UnitSeed("TRAY", "Tray"),
            new UnitSeed("BOX", "Box"),
            new UnitSeed("PACK", "Pack"),
            new UnitSeed("PACKET", "Packet"),
            new UnitSeed("POUCH", "Pouch"),
            new UnitSeed("SACHET", "Sachet"),
            new UnitSeed("BAG", "Bag"),
            new UnitSeed("BUNDLE", "Bundle"),
            new UnitSeed("ROLL", "Roll"),
            new UnitSeed("CAN", "Can"),
            new UnitSeed("TIN", "Tin"),
            new UnitSeed("JAR", "Jar"),
            new UnitSeed("BOTTLE", "Bottle"),
            new UnitSeed("CARTON", "Carton"),
            new UnitSeed("CASE", "Case"),
            new UnitSeed("CRATE", "Crate"),
            new UnitSeed("BUCKET", "Bucket"),
            new UnitSeed("TUB", "Tub"),
            new UnitSeed("KG", "Kilogram"),
            new UnitSeed("G", "Gram"),
            new UnitSeed("MG", "Milligram"),
            new UnitSeed("LB", "Pound"),
            new UnitSeed("OZ", "Ounce"),
            new UnitSeed("L", "Litre"),
            new UnitSeed("ML", "Millilitre"),
            new UnitSeed("CL", "Centilitre"),
            new UnitSeed("GAL", "Gallon"),
            new UnitSeed("QT", "Quart"),
            new UnitSeed("PT", "Pint"),
            new UnitSeed("CUP", "Cup"),
            new UnitSeed("TBSP", "Tablespoon"),
            new UnitSeed("TSP", "Teaspoon"),
            new UnitSeed("M", "Metre"),
            new UnitSeed("CM", "Centimetre"),
            new UnitSeed("MM", "Millimetre"),
            new UnitSeed("FT", "Foot"),
            new UnitSeed("IN", "Inch"),
            new UnitSeed("SERVING", "Serving"),
            new UnitSeed("PORTION", "Portion"),
            new UnitSeed("SLICE", "Slice"),
            new UnitSeed("LOAF", "Loaf"),
            new UnitSeed("HEAD", "Head"),
            new UnitSeed("BOWL", "Bowl"),
            new UnitSeed("PLATE", "Plate"),
            new UnitSeed("GLASS", "Glass"),
            new UnitSeed("SHOT", "Shot")
    );

    @Transactional
    public void seedUnitMasters() {
        for (UnitSeed seed : DEFAULT_UNITS) {
            unitMasterRepository.findByCodeIgnoreCase(seed.code())
                    .orElseGet(() -> {
                        UnitMaster unitMaster = new UnitMaster();
                        unitMaster.setCode(seed.code());
                        unitMaster.setDisplayName(seed.displayName());
                        unitMaster.setActive(true);
                        return unitMasterRepository.save(unitMaster);
                    });
        }
    }

    private record UnitSeed(String code, String displayName) {
    }
}
