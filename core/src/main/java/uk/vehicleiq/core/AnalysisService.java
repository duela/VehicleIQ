package uk.vehicleiq.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AnalysisService {
    private final AnalysisRepository analyses;
    private final VehicleRepository vehicles;
    private final OutboxEventRepository outbox;
    private final AuditEventRepository audit;
    private final ObjectMapper mapper;
    AnalysisService(AnalysisRepository analyses,VehicleRepository vehicles,OutboxEventRepository outbox,AuditEventRepository audit,ObjectMapper mapper){
        this.analyses=analyses;this.vehicles=vehicles;this.outbox=outbox;this.audit=audit;this.mapper=mapper;
    }

    @Transactional
    AnalysisEntity request(String vehicleId,String tenantId,String actor){
        VehicleEntity vehicle=vehicles.findById(vehicleId).filter(v->v.getTenantId().equals(tenantId)).orElseThrow(()->new NoSuchElementException("Vehicle not found"));
        AnalysisEntity analysis=analyses.save(new AnalysisEntity(tenantId,vehicle.getId()));
        outbox.save(new OutboxEventEntity("analysis.requested.v1",analysis.getId(),"{\"analysisId\":\""+analysis.getId()+"\",\"tenantId\":\""+tenantId+"\"}"));
        audit.save(new AuditEventEntity(tenantId,actor,"analysis.requested",analysis.getId()));
        return analysis;
    }

    @Transactional
    AnalysisEntity complete(String analysisId){
        AnalysisEntity analysis=analyses.findById(analysisId).orElseThrow(()->new NoSuchElementException("Analysis not found"));
        if("COMPLETED".equals(analysis.getStatus())) return analysis;
        VehicleEntity vehicle=vehicles.findById(analysis.getVehicleId()).orElseThrow(()->new NoSuchElementException("Vehicle not found"));
        try {
            List<Double> comps=mapper.readValue(vehicle.getComparablePricesJson(),new TypeReference<>(){});
            List<Double> sorted=comps.stream().filter(Objects::nonNull).filter(v->v>0).sorted().toList();
            Map<String,Object> valuation=new LinkedHashMap<>();
            double low=0,mid=0,high=0; boolean hasComps=!sorted.isEmpty();
            if(hasComps){
                low=sorted.get(0); high=sorted.get(sorted.size()-1);
                if(sorted.size()%2==1) mid=sorted.get(sorted.size()/2); else mid=(sorted.get(sorted.size()/2-1)+sorted.get(sorted.size()/2))/2.0;
                if(sorted.size()==1){low=Math.round(mid*.95);high=Math.round(mid*1.05);}
                valuation.put("low",Math.round(low));valuation.put("midpoint",Math.round(mid));valuation.put("high",Math.round(high));
                valuation.put("confidence",sorted.size()>=5?"moderate":"limited");
            } else { valuation.put("low",null);valuation.put("midpoint",null);valuation.put("high",null);valuation.put("confidence","insufficient_data"); }
            valuation.put("currency","GBP");valuation.put("basis",hasComps?sorted.size()+" user-provided comparable price(s)":"No comparable prices supplied");valuation.put("source","user_input");
            double costs=vehicle.getPurchasePrice()+vehicle.getBuyerFees()+vehicle.getTransport()+vehicle.getRepairEstimate();
            List<Map<String,Object>> risks=new ArrayList<>();
            if(!hasComps) risks.add(signal("data_gap","review","Add recent comparable vehicles to calculate a valuation range."));
            if(vehicle.getConditionNotes()==null||vehicle.getConditionNotes().isBlank()) risks.add(signal("condition_evidence","review","No condition notes or inspection evidence were supplied."));
            risks.add(signal("external_history","information","No licensed history provider is configured; no external check was run."));
            Map<String,Object> economics=Map.of("estimatedCosts",Math.round(costs),"currency","GBP",
                    "estimatedGrossMarginLow",hasComps?Math.round(low-costs):0,"estimatedGrossMarginHigh",hasComps?Math.round(high-costs):0,
                    "marginAvailable",hasComps);
            Map<String,Object> condition=Map.of("status",vehicle.getConditionNotes()!=null&&!vehicle.getConditionNotes().isBlank()?"USER_NOTES_ADDED":"NEEDS_REVIEW",
                    "notes",vehicle.getConditionNotes()==null?"":vehicle.getConditionNotes(),"automatedImageAssessment",false);
            Map<String,Object> verification=Map.of("status","UNAVAILABLE","checks",List.of(),"reason","No authoritative data source is configured for this demo.");
            Map<String,Object> result=new LinkedHashMap<>();
            result.put("valuation",valuation);result.put("economics",economics);result.put("riskSignals",risks);result.put("condition",condition);result.put("verification",verification);
            result.put("provenance",Map.of("engineVersion","rules-1.0.0","calculatedAt",Instant.now().toString(),"input","tenant-submitted data"));
            result.put("limitations",List.of("This is decision support, not a guaranteed sale price.","No live auction, marketplace or vehicle-history integration is connected."));
            analysis.setResultJson(mapper.writeValueAsString(result));analysis.setStatus("COMPLETED");analysis.setCompletedAt(Instant.now());
            audit.save(new AuditEventEntity(analysis.getTenantId(),"system","analysis.completed",analysis.getId()));
            return analysis;
        } catch(Exception e){ throw new IllegalStateException("Could not compute analysis",e); }
    }
    private Map<String,Object> signal(String category,String severity,String explanation){return Map.of("category",category,"severity",severity,"explanation",explanation,"source","vehicleiq_rules");}
}

