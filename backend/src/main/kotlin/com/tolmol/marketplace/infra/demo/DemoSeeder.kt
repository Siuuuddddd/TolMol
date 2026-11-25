package com.tolmol.marketplace.infra.demo

import com.tolmol.platform.infra.config.Databases
import com.tolmol.marketplace.domain.model.*
import com.tolmol.marketplace.service.*
import com.tolmol.marketplace.infra.redis.ListingGeoIndex
import com.tolmol.user.service.*
import com.tolmol.user.domain.model.*
import com.tolmol.user.domain.model.Role
import com.tolmol.bidding.service.*
import com.tolmol.bidding.domain.model.Bid
import com.tolmol.chat.service.ChatService
import com.tolmol.deal.service.DealService
import com.tolmol.deal.domain.model.DealState
import com.tolmol.notification.service.AlertStore
import com.tolmol.notification.domain.model.Alert
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.*
import org.springframework.core.env.*
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import java.time.Instant
import java.sql.Timestamp
import java.util.UUID
import kotlin.math.*

class DemoEnabled : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata) =
        context.environment.acceptsProfiles(Profiles.of("demo")) ||
            context.environment.getProperty("tolmol.seed.demo",Boolean::class.java,false)
}

@Component
@Conditional(DemoEnabled::class)
class DemoSeeder(
    private val db: Databases, private val auth: AuthService, private val users: UserStore,
    private val listings: ListingStore, private val market: MarketplaceService,
    private val bids: BidStore, private val bidding: BiddingService, private val chat: ChatService,
    private val deals: DealService, private val alerts: AlertStore,
    private val redis: StringRedisTemplate, private val env: Environment, private val geoIndex: ListingGeoIndex,
) : ApplicationRunner {
    private val log=LoggerFactory.getLogger(javaClass)
    private fun id(key: String) = UUID.nameUUIDFromBytes(("tolmol.demo:"+key).toByteArray())
    private val names=listOf("Aarav Sharma","Priya Deshmukh","Rohan Mehta","Ananya Patil","Vikram Shah","Neha Joshi",
        "Kabir Rao","Isha Kulkarni","Arjun Nair","Sana Khan","Dev Patel","Meera Iyer","Aditya Singh","Kavya Menon",
        "Rahul Gupta","Aditi Jain","Nikhil Sethi","Pooja Bhat","Sameer Das","Riya Kapoor")
    private val sellerIndices=listOf(0,1,2,3,4,5,6,7,16,17,18,19)
    private val buyerIndices=listOf(8,9,10,11,12,13,14,15,16,17,18,19)
    private data class Item(val title: String,val category: String,val kg: String,val rate: String,val key: String?,val description: String)
    private val items=listOf(
        Item("Bundled old newspapers","PAPER","35","14","scrap_paper_01","Dry newspapers tied in bundles. Ready for collection from our building."),
        Item("Newspaper stacks from an office","PAPER","65","13","scrap_paper_02","Stored newspaper bundles from an office clean-out. Sold together by estimated weight."),
        Item("Mixed rusty iron fittings","METAL","85","29","scrap_metal_01","Mixed rusty metal pipes, fittings and offcuts. Buyer should inspect and sort at pickup."),
        Item("Iron rods and offcuts","METAL","140","32","scrap_metal_02","Construction iron rod offcuts in mixed lengths. Please bring suitable loading transport."),
        Item("Copper wire scrap","METAL","18","540","scrap_metal_04","Mixed copper wire offcuts from electrical work. Final weight and condition checked at pickup."),
        Item("Aluminium sheet offcuts","METAL","28","130","scrap_metal_06","Mixed aluminium sheet offcuts from fabrication. Edges may be sharp; collect with gloves."),
        Item("Old steel kitchen utensils","METAL","22","45","scrap_metal_08","Used stainless steel kitchen utensils for scrap recovery. Sold as a mixed lot."),
        Item("Mixed brass fittings","METAL","12","340","scrap_metal_10","Old brass plumbing fittings from a renovation. Rate is per kg after inspection."),
        Item("Used cardboard cartons","CARDBOARD","42","9","scrap_cardboard_01","Empty used cardboard cartons in mixed sizes. Some boxes have tape and printed labels."),
        Item("Flattened corrugated cardboard","CARDBOARD","55","10","scrap_cardboard_02","Flattened cardboard boxes stacked for recycling. Dry storage and easy loading access."),
        Item("Plastic bottles for recycling","PLASTIC","16","17",null,"Empty mixed plastic bottles from a residential collection. Sorting is required at pickup."),
        Item("Used plastic storage crates","PLASTIC","32","21",null,"Used plastic storage crates with wear and a few cracked pieces. Sold by total weight."),
        Item("Used blue paint drums","PAINT","75","16","scrap_paint_01","Used blue paint drums, some with residue. Please inspect their condition before reuse or recycling."),
        Item("Empty blue industrial paint drums","PAINT","95","15","scrap_paint_02","Blue industrial paint containers stored indoors. Residue and suitability must be checked at pickup."),
        Item("Leftover paint buckets","PAINT","48","12","scrap_paint_03","Mixed used paint buckets with leftover material. Condition and remaining contents vary."),
        Item("Used top-load washing machine","ELECTRONICS","34","28","scrap_electronics_01","Used top-load washing machine sold as-is for repair or parts. Working condition is not guaranteed."),
        Item("Wrapped used washing machine","ELECTRONICS","38","27","scrap_electronics_02","Used top-load washing machine wrapped for storage. Inspect the appliance before collecting."),
        Item("Mixed cables and small e-waste","ELECTRONICS","24","45","scrap_electronics_03","Mixed used cables, adapters and small electronic scrap. Sold unsorted for material recovery."),
        Item("Old computer parts and e-waste","ELECTRONICS","46","38","scrap_electronics_04","Mixed old computer components and electronic scrap. Data-bearing parts should be checked by the seller."),
        Item("Old CRT monitors and keyboards","ELECTRONICS","70","18","scrap_electronics_05","Old CRT monitors, keyboards and cables sold as e-waste. Handle glass screens carefully."),
        Item("Old refrigerator for parts","ELECTRONICS","48","24",null,"Old refrigerator sold for repair or parts. Arrange safe handling of refrigerant and transport."),
        Item("Used ceiling fans","ELECTRONICS","18","35",null,"Three used ceiling fans from renovation work. Sold as-is for repair or recycling."),
        Item("Old mixer and small appliances","ELECTRONICS","12","32",null,"Used mixer and assorted small kitchen appliances. Working condition is not guaranteed."),
        Item("Used two-seat sofa","FURNITURE","42","8",null,"Used fabric sofa with visible wear. Buyer arranges loading and transport."),
        Item("Old wooden chairs","FURNITURE","28","9",null,"Four old wooden chairs with wear. Inspect joints and condition at pickup."),
        Item("Used wooden table","FURNITURE","36","10",null,"Used wooden table from an office. Some scratches and wear; sold as-is."),
        Item("Old wardrobe for reuse","FURNITURE","65","7",null,"Used wooden wardrobe with surface wear. Collection may require disassembly."),
        Item("Mixed textile offcuts","OTHER","25","6","scrap_other_01","Mixed fabric strips and small textile offcuts. Available as an unsorted recycling lot."),
        Item("Colourful fabric remnants","OTHER","40","7","scrap_other_02","Assorted textile remnants in multiple colours. Composition and sizes vary."),
        Item("Surplus cement sacks","OTHER","150","3","scrap_other_03","Stored cement sacks from a building site. Inspect material age and condition; not a paper lot."),
        Item("White cement surplus sacks","OTHER","80","4","scrap_other_04","White cement sacks sold as surplus material. Suitability and condition must be checked before use.")
    )
    override fun run(args: ApplicationArguments) {
        check(!env.acceptsProfiles(Profiles.of("prod","production"))) { "Demo data is forbidden with a production profile" }
        if(env.getProperty("tolmol.seed.reset",Boolean::class.java,false)) reset()
        if(!env.getProperty("tolmol.seed.reset-only",Boolean::class.java,false)) seed()
    }
    @Synchronized fun seed() {
        check(!env.acceptsProfiles(Profiles.of("prod","production")))
        val lat=env.getProperty("tolmol.seed.center-lat",Double::class.java,19.0760)
        val lng=env.getProperty("tolmol.seed.center-lng",Double::class.java,72.8777)
        val now=Instant.now()
        val people=names.mapIndexed { index,name ->
            val email="demo.user%02d@tolmol.demo".format(index+1)
            val user=users.find(email)?.id ?: auth.register(email,"Demo@12345",setOf(Role.BUYER,Role.SELLER),name)
            db.users.sql.sql("update accounts set email_verified=true,joined_at=coalesce(joined_at,?) where id=?")
                .params(Timestamp.from(now.minusSeconds((index+365L)*86400)),user).update()
            val radius=0.3+(index%10)*0.8
            val angle=index*2*PI/20
            val position=Position(lat+radius*cos(angle)/111.32,lng+radius*sin(angle)/(111.32*cos(lat*PI/180)))
            auth.locate(user,Role.BUYER,position); auth.locate(user,Role.SELLER,position)
            user
        }
        val lots=(0 until 53).map { index ->
            val item=items[index%items.size]
            val lotId=id("lot:$index")
            // Use both supplied photos across the repeated demo lots for each material.
            val imageKey=if(index<items.size) item.key else when(item.key) {
                "scrap_metal_02" -> "scrap_metal_03"
                "scrap_metal_04" -> "scrap_metal_05"
                "scrap_metal_06" -> "scrap_metal_07"
                "scrap_metal_08" -> "scrap_metal_09"
                "scrap_metal_10" -> "scrap_metal_11"
                else -> item.key
            }
            val deadline=now.plusSeconds(if(index<5) 2700L+index*600 else 7200L+(index*4700L)%248400L)
            val seller=people[sellerIndices[index%sellerIndices.size]]
            val point=auth.position(seller,Role.SELLER)
            if(listings.get(lotId)==null) {
                listings.insert(Listing(lotId,seller,item.category,item.kg.toBigDecimal(),item.rate.toBigDecimal(),
                    point.latitude,point.longitude,deadline,"OPEN",0,item.title,item.description,imageKey))
            } else {
                // Preserve participant activity and terminal states; refresh only still-open demo lots.
                db.marketplace.sql.sql("update listings set deadline=?,latitude=?,longitude=?,image_key=?,version=version+1 where id=? and status='OPEN'")
                    .params(Timestamp.from(deadline),point.latitude,point.longitude,imageKey,lotId).update()
            }
            listings.get(lotId)!!.also { geoIndex.add(it) }
        }
        var serial=0
        fun history(index: Int,buyer: UUID,step: Int): Bid? {
            val lot=lots[index]
            if(lot.seller==buyer || listings.get(lot.id)!!.status!="OPEN") return null
            val key=id("bid:$index:$step")
            serial++
            market.eligible(buyer,lot.id)
            return bids.atomic {
                bids.lockBuyer(buyer)
                val previous=bids.previous(buyer,key)
                if(previous!=null) return@atomic previous
                val auction=bids.auction(lot.id,true)
                var created=now.minusSeconds(172800).plusSeconds(serial*60L)
                val amount=lot.startingRate+step.toBigDecimal()
                val next=bids.nextBid(buyer,lot.seller)
                if(next!=null && created<next) created=next
                val leading=bids.leading(lot.id)
                if(leading!=null && created<=leading.createdAt) created=leading.createdAt.plusSeconds(1)
                check(created<now) { "Demo history must precede startup" }
                check(leading==null || amount>leading.rate)
                val bid=Bid(id("bid-id:$index:$step"),lot.id,buyer,amount,key,created)
                bids.save(bid,lot.seller)
                bids.change(auction,"OPEN","BID_PLACED",bid)
                bid
            }
        }
        for(index in 0 until 30) for(step in 1..(1+index%4)) {
            val candidates=buyerIndices.map { people[it] }.filter { it!=lots[index].seller }
            history(index,candidates[(index+step)%candidates.size],step)
        }
        for(index in 50..52) {
            val lot=listings.get(lots[index].id)!!
            if(lot.status=="OPEN") history(index,people[8],1)?.let { bidding.accept(lot.seller,lot.id,it.id) }
            val deal=deals.find(people[8],lot.id)
            if(index==51 && deal.state==DealState.PENDING_PICKUP && deal.actualWeight==null)
                deals.inspect(people[8],lot.id,lot.weight+1.toBigDecimal())
            if(index==52 && deal.state==DealState.PENDING_PICKUP) {
                val measured=if(deal.actualWeight==null) deals.inspect(people[8],lot.id,lot.weight) else deal
                deals.confirm(lot.seller,lot.id,measured.revision)
            }
        }
        for(index in 0 until 6) {
            val lot=lots[index]
            if(listings.get(lot.id)!!.status!="OPEN") continue
            val thread=chat.open(people[8],lot.id)
            val messages=listOf("Hi, is this lot still available?","Yes, collection is possible tomorrow.",
                "Can I inspect it before collection?","Yes, please message before you arrive.")
            messages.forEachIndexed { n,body -> chat.send(if(n%2==0) people[8] else lot.seller,thread.id,body,id("message:$index:$n")) }
        }
        for(buyerIndex in buyerIndices) {
            val user=people[buyerIndex]
            lots.filter { it.seller!=user && listings.get(it.id)!!.status=="OPEN" }.take(3).forEach { market.watch(user,it.id,true) }
            val alert=Alert(id("welcome:$buyerIndex"),"LISTING_AVAILABLE",lots[0].id,mapOf("category" to lots[0].category))
            alerts.pending(user,alert); alerts.delivered(user,alert.eventId)
        }
        log.info("DEMO ONLY: seller demo.user01@tolmol.demo; buyer demo.user09@tolmol.demo; both demo.user17@tolmol.demo; password Demo@12345; center {},{}",lat,lng)
    }

    @Synchronized fun reset() {
        check(!env.acceptsProfiles(Profiles.of("prod","production")))
        val people=db.users.sql.sql("select id from accounts where email like '%@tolmol.demo'").query(UUID::class.java).list()
        if(people.isEmpty()) return
        val lotIds=db.marketplace.sql.sql("select id from listings where seller in (:users)").param("users",people).query(UUID::class.java).list()
            .ifEmpty { listOf(UUID(0,0)) }
        check(db.marketplace.sql.sql("select count(*) from bids where buyer in (:users) and listing not in (:lots)")
            .param("users",people).param("lots",lotIds).query(Long::class.java).single()==0L) { "Reset refuses demo interactions on non-demo listings" }
        val threadIds=db.chat.sql.sql("select id from threads where seller in (:users) or buyer in (:users)")
            .param("users",people).query(UUID::class.java).list().ifEmpty { listOf(UUID(0,0)) }
        fun marketDelete(sql: String) = db.marketplace.sql.sql(sql).param("users",people).param("lots",lotIds).update()
        db.chat.transactions.executeWithoutResult {
            db.chat.sql.sql("delete from messages where thread_id in (:ids)").param("ids",threadIds).update()
            db.chat.sql.sql("delete from outbox where aggregate_id in (:ids)").param("ids",threadIds).update()
            db.chat.sql.sql("delete from threads where id in (:ids)").param("ids",threadIds).update()
        }
        db.marketplace.transactions.executeWithoutResult {
            marketDelete("update listings set winning_bid=null where id in (:lots)")
            marketDelete("delete from deals where listing in (:lots)")
            marketDelete("delete from watchers where buyer in (:users) or listing in (:lots)")
            marketDelete("delete from muted_listings where user_id in (:users) or listing in (:lots)")
            marketDelete("delete from notification_preferences where user_id in (:users)")
            marketDelete("delete from alerts where recipient in (:users) or aggregate_id in (:lots)")
            db.marketplace.sql.sql("delete from alerts where aggregate_id in (:ids)").param("ids",threadIds).update()
            marketDelete("delete from outbox where aggregate_id in (:lots)")
            db.marketplace.sql.sql("delete from outbox where aggregate_id in (:ids)").param("ids",threadIds).update()
            marketDelete("delete from role_activity_outbox where user_id in (:users) or listing_id in (:lots)")
            marketDelete("delete from bids where listing in (:lots)")
            marketDelete("delete from listings where id in (:lots)")
        }
        db.location.sql.sql("delete from positions where user_id in (:ids)").param("ids",people).update()
        db.users.transactions.executeWithoutResult {
            for(table in listOf("buyer.activity","seller.activity"))
                db.users.sql.sql("delete from $table where user_id in (:users) or listing_id in (:lots)").param("users",people).param("lots",lotIds).update()
            for((table,column) in listOf("devices" to "owner_id","social_identities" to "user_id","account_profiles" to "user_id","accounts" to "id"))
                db.users.sql.sql("delete from $table where $column in (:ids)").param("ids",people).update()
        }
        redis.opsForZSet().remove("tolmol:geo:listings",*lotIds.map { it.toString() }.toTypedArray())
        for(key in lotIds.flatMap { redis.keys("tolmol:auction:$it:*") }+threadIds.flatMap { redis.keys("tolmol:chat:$it:*") }+
            people.flatMap { redis.keys("tolmol:location:$it:*") }) redis.delete(key)
    }
}
