package uk.vehicleiq.core;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DemoData {
    @Bean CommandLineRunner seedVehicleIq(VehicleRepository vehicles,AnalysisService analyses){return args->{
        if(vehicles.count()>0)return;
        var a=vehicle("tenant-demo","AB21VQX","BMW","3 Series 320d M Sport",2021,38400,18200,410,160,1250,List.of(23950.0,24750.0,25100.0),"Seller notes: light scuff to rear bumper. Photo evidence added.");
        var b=vehicle("tenant-demo","YX19KRP","Volkswagen","Golf GTI Performance",2019,51200,15750,345,120,800,List.of(20995.0,21400.0,21950.0),"");
        var c=vehicle("tenant-demo","LM23JTW","Audi","A3 Sportback S line",2023,18100,20200,450,0,550,List.of(23900.0,24450.0,24900.0,24600.0),"Service history described by seller; supporting document not yet uploaded.");
        vehicles.saveAll(List.of(a,b,c));
        var other=vehicle("tenant-north","GD20HTR","Ford","Focus ST-Line X",2020,42900,13900,320,190,600,List.of(17995.0,18500.0),"Admin view sample tenant record.");
        vehicles.save(other);
        analyses.request(a.getId(),a.getTenantId(),"demo.seed");
        analyses.request(b.getId(),b.getTenantId(),"demo.seed");
        analyses.request(c.getId(),c.getTenantId(),"demo.seed");
        analyses.request(other.getId(),other.getTenantId(),"demo.seed");
    };}
    private VehicleEntity vehicle(String tenant,String reg,String make,String model,int year,int miles,double buy,double fees,double transport,double repair,List<Double> comps,String notes){
        VehicleEntity v=new VehicleEntity();v.setTenantId(tenant);v.setRegistration(reg);v.setMake(make);v.setModel(model);v.setYear(year);v.setMileage(miles);v.setFuelType("Diesel");v.setTransmission("Automatic");v.setColour("Blue");v.setPurchasePrice(buy);v.setBuyerFees(fees);v.setTransport(transport);v.setRepairEstimate(repair);v.setConditionNotes(notes);try{v.setComparablePricesJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(comps));}catch(Exception e){throw new IllegalStateException(e);}return v;
    }
}

