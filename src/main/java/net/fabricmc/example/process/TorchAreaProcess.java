package net.fabricmc.example.process;

import baritone.api.IBaritone;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.selection.ISelection;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.light.BlockLightStorage;
import net.minecraft.world.chunk.light.ChunkLightProvider;

import java.util.*;


/*
1. Every n ticks, update the entire locations list
2. Every time a light source is placed, update the locations in a 3x3 chunk section
around the player

Every tick, check if the player is next to a block with low light level. If it is,
perform logic based on the types of light blocks available.
    Logic depends on air vs. water vs. both and what faces of block it can be placed on

Possible Light Blocks:
Candles (Air)
Campfire + Soul Campfire
End Rod (Air)
Glowstone
Shroomlight
Froglight (3 types)
Jack O' Lantern
Lantern
Soul Lantern
Sea Lantern
Sea Pickle
Soul Torch
Torch

Original List:
Torch
Lantern


 */


//TODO: Instead of just doing chunkX and chunkZ, include "chunkY" as well (10 bits per axis)
//This is because WorldScanner isn't even available via Baritone API, so we might as well just make our
//lives easier by including y in the first place.

public class TorchAreaProcess implements IBaritoneProcess, Helper {

    public static final int MAX_AXIS_LENGTH = (1 << 10)-1;

    private IBaritone baritone;
    private IPlayerContext ctx;

    private BlockPos center;
    private int range;

    private boolean active;

    //Store the selection itself
    private ISelection[] selections;

    //Store Min of all Selections in composite selection

    private BetterBlockPos minPos;

    //Store Map of Collection<BlockPos> for each chunk in the
    //current selection (reset when selection is changed)
        //Integer should be chunkX-minX and chunkZ-minZ concat
        //Element is Collection<BlockPos>

    private Map<Integer, Collection<BlockPos>> goalBlocks;

    //Store Last Place Pos

    public TorchAreaProcess(IBaritone baritone) {
        this.baritone = baritone;
        this.ctx = baritone.getPlayerContext();
    }


    public void lightArea(ISelection ...sels) {

        if(sels.length <= 0) {
            logDirect("No selection");
            onLostControl();
            throw new RuntimeException("No Selection Present");
        }

        active = true;
        this.selections = sels;

        minPos = null;
        goalBlocks = new HashMap<Integer, Collection<BlockPos>>();

        //Calculate Min Pos of Selections
        minPos = sels[0].min();
        for(int i = 1; i < sels.length; i++) {
            BetterBlockPos minI = sels[i].min();
            minPos = new BetterBlockPos(
                    Math.min(minPos.x, minI.x),
                    Math.min(minPos.y, minI.y),
                    Math.min(minPos.z, minI.z)
            );
        }

        //Ensure total selections don't have more than 65535 blocks in any axis
            //Use min and max of each selection to help
        BetterBlockPos tempMaxPos = sels[0].max();
        for(int i =1; i < sels.length; i++) {
            BetterBlockPos maxI = sels[i].max();
            tempMaxPos = new BetterBlockPos(
                    Math.max(tempMaxPos.x, maxI.x),
                    Math.max(tempMaxPos.y, maxI.y),
                    Math.max(tempMaxPos.z, maxI.z)
            );
        }

        if(tempMaxPos.x - minPos.x > MAX_AXIS_LENGTH ||
        tempMaxPos.y - minPos.y > MAX_AXIS_LENGTH ||
        tempMaxPos.z - minPos.z > MAX_AXIS_LENGTH
        ) {
            logDirect("Selection too large");
            onLostControl();
            throw new RuntimeException("Selection too large. All selections must have a length fitting in 15 bits for x and z individually");
        }

        //Calculate the Chunks to Check and Store Them
        int minChunkX = minPos.x >> 4;
        int minChunkY = minPos.y >> 4;
        int minChunkZ = minPos.z >> 4;
        int maxChunkX = tempMaxPos.x >> 4;
        int maxChunkY = tempMaxPos.y >> 4;
        int maxChunkZ = tempMaxPos.z >> 4;


        //TODO: Double check this. It's kinda sus imo
        int chunkX = minChunkX;
        int chunkY = minChunkY-1;
        int chunkZ = minChunkZ;
        while(chunkX <= maxChunkX && chunkZ <= maxChunkZ) {

            //Iterate to next chunk


            chunkY++;
            if(chunkY > maxChunkY) {
                chunkY = minChunkY;
                chunkX++;
                if(chunkX > maxChunkX) {
                    chunkX = minChunkX;
                    chunkZ++;
                }
            }

            boolean isValidChunk = false;
            for(ISelection selection : sels) {
                if(isChunkInSelection(selection, chunkX, chunkY, chunkZ)) {
                    isValidChunk = true;
                }
            }

            if(!isValidChunk) {
                continue;
            }

            if((chunkX-minChunkX) > MAX_AXIS_LENGTH || (chunkZ-minChunkZ) > MAX_AXIS_LENGTH) {
                logDirect("Something went wrong with chunk indexing");
                onLostControl();
                throw new RuntimeException("Chunk offset exceeded 15 bits. This is likely because the selection is too large.");
            }

            int chunkIndex = ((chunkX-minChunkX) << 20) & ((chunkZ-minChunkZ) << 10) & (chunkY-minChunkY);
            goalBlocks.put(chunkIndex, new ArrayList<BlockPos>());
        }

        //populateFullSelection
        populateFullSelection();
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public PathingCommand onTick(boolean b, boolean b1) {

        //If tick count % n is 0, refresh the whole lighting thing

        //Else if last place pos isn't null, update 3x3 chunk radius around it

        //Check if block standing on or any adjacent blocks on floor/1 up
        //Meet conditions (block light level 0, full block, air above)
        //Use shouldLightBlock
            //If so, place light source on block (look at FarmProcess for help)
                //This might involve some inventory work as well
            //Update the last place pos

        //Create a GoalComposite with all the blocks that we should go to
            //Try to make this only include those in a certain chunk radius

        //NOTE: Path should be the one that goes adjacent to the block not
        //on the block. You can specify that you want to be looking at
        //the top face in the PathingCommand


        return null;
    }

    //Loop through all chunks and update them
    private void populateFullSelection() {
        for(int chunkIndex : goalBlocks.keySet()) {
            populateChunk(chunkIndex);
        }
    }

    //Take in the chunk index from the map and populate it with BlockPos
    //Use WorldScanner (chunkInto) to get Air Blocks
    //Filter to only those with a FULL block underneath with block light level 0
    //Also filter to ensure it is in the selection
    //Use shouldLightBlock
    private void populateChunk(int chunkIdx) {
        int chunkX = chunkIdx & ((1 << 30)-1) ^ ((1 << 20)-1);
        int chunkY = chunkIdx & ((1 << 10)-1);
        int chunkZ = chunkIdx & ((1 << 20)-1) ^ ((1 << 10)-1);

        int originX = chunkX << 4;
        int originY = chunkY << 4;
        int originZ = chunkZ << 4;

        Chunk chunk = baritone.getPlayerContext().world().getChunk(chunkX, chunkZ);

        ChunkLightProvider<?,?> blockLightProvider = baritone.getPlayerContext().world().getLightingProvider().blockLightProvider;

        if(blockLightProvider == null) {
            throw new RuntimeException("Unable to access block light provider");
        }

        if(chunk instanceof EmptyChunk) {
            return;
        }

        for(int xOff = 0; xOff < 15; xOff++) {
            for(int yOff = 0; yOff < 15; yOff++) {
                for(int zOff = 0; zOff < 15; zOff++) {
                    BlockPos pos = new BlockPos(originX+xOff, originY+yOff, originZ+zOff);
                    if(shouldLightBlock(chunk, pos)) {
                        goalBlocks.get(chunkIdx).add(pos);
                    }
                }
            }
        }
    }

    //Check for full block below, air , block below light level 0
    private boolean shouldLightBlock(Chunk chunk, BlockPos pos) {

        BlockPos belowBlock = pos.add(0,-1,0);
        ChunkLightProvider<?,?> blockLightProvider = baritone.getPlayerContext().world().getLightingProvider().blockLightProvider;

        if(blockLightProvider == null) {
            return false;
        }


        return chunk.getBlockState(pos).isAir()
                && chunk.getBlockState(belowBlock).isFullCube(baritone.getPlayerContext().world(), belowBlock)
                && (blockLightProvider.getLightLevel(belowBlock) == 0);
    }

    private boolean isChunkInSelection(ISelection selection, int chunkX, int chunkY, int chunkZ) {
        BetterBlockPos min = selection.min();
        BetterBlockPos max = selection.max();

        return (min.x >> 4) >= chunkX && (min.z >> 4) >= chunkZ && (min.y >> 4) >= chunkY
                && (max.x >> 4) <= chunkX && (max.z >> 4) <= chunkZ && (max.y >> 4) <= chunkY;
    }

    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public void onLostControl() {
        active = false;
    }

    @Override
    public String displayName0() {
        return "Lighting Area";
    }
}
