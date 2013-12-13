import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderCreeper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.lang3.text.WordUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Derived from Vanilla's GuiContainerCreative
 */

@SideOnly(Side.CLIENT)
public class GuiBlockWiki extends InventoryEffectRenderer{
    private static String currentFile = "";
    private static List<String> fileInfo = new ArrayList<String>();
    private static ItemStack drawingStack;
    private static EnumWikiSection curSection = EnumWikiSection.BLOCK_AND_ITEM;
    private static Render curEntityRenderer = new RenderCreeper();
    private static Entity curEntity;

    static {
        curEntityRenderer.setRenderManager(RenderManager.instance);
    }

    private enum EnumWikiSection{
        BLOCK_AND_ITEM, ENTITIES
    }

    private final List<LocatedStack> locatedStacks = new ArrayList<LocatedStack>();
    private final List<LocatedString> locatedStrings = new ArrayList<LocatedString>();
    private final RenderItem customItemRenderer;

    private static InventoryBasic inventory = new InventoryBasic("tmp", true, 36);
    private static final ResourceLocation field_110424_t = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");

    /** Amount scrolled in Creative mode inventory (0 = top, 1 = bottom) */
    private float currentScroll;

    /** True if the scrollbar is being dragged */
    private boolean isScrolling;

    /**
     * True if the left mouse button was held down last time drawScreen was called.
     */
    private boolean wasClicking;
    private GuiTextField searchField;

    private static final int SCROLL_X = 80;
    private static final int SCROLL_HEIGHT = 162;
    private static final int SCROLL_Y = 66;
    private static final int WRAP_LENGTH = 55;
    private static final double TEXT_SCALE = 0.5;
    private static final int TEXT_START_X = 100;
    private static final int TEXT_START_Y = 10;

    public GuiBlockWiki(){
        super(new ContainerBlockWiki());
        EntityPlayer player = FMLClientHandler.instance().getClient().thePlayer;
        player.openContainer = inventorySlots;
        allowUserInput = true;
        ySize = 238;
        xSize = 256;

        customItemRenderer = new RenderItem();
        customItemRenderer.setRenderManager(RenderManager.instance);
        curEntity = new EntityCreeper(player.worldObj);
    }

    @Override
    protected void handleMouseClick(Slot slot, int x, int y, int mouse){
        if(slot != null && slot.getHasStack()) {
            setCurrentFile(Paths.WIKI_PATH + slot.getStack().getUnlocalizedName().replace("tile.", "block/").replace("item.", "item/"), slot.getStack());
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button){
        super.mouseClicked(x, y, button);
        for(LocatedStack locatedStack : locatedStacks) {
            if(locatedStack.isMouseWithinRegion(x - 84, y)) {
                setCurrentFile(Paths.WIKI_PATH + locatedStack.stack.getUnlocalizedName().replace("tile.", "block/").replace("item.", "item/"), locatedStack.stack);
                break;
            }
        }
        for(int i = 0; i < EnumWikiSection.values().length; i++) {
            if(x <= 33 + guiLeft && x >= 1 + guiLeft && y >= 8 + guiTop + i * 35 && y <= 43 + guiTop + i * 35) {
                curSection = EnumWikiSection.values()[i];
            }
        }

    }

    public void setCurrentFile(String fileName, ItemStack stack){
        currentFile = fileName;
        fileInfo = InfoSupplier.getInfo(fileName);
        drawingStack = stack;
        updateWikiPage();
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    @Override
    public void initGui(){
        super.initGui();
        buttonList.clear();
        Keyboard.enableRepeatEvents(true);
        searchField = new GuiTextField(fontRenderer, guiLeft + 40, guiTop + 52, 53, fontRenderer.FONT_HEIGHT);
        searchField.setMaxStringLength(15);
        searchField.setEnableBackgroundDrawing(true);
        searchField.setVisible(true);
        searchField.setFocused(true);
        searchField.setCanLoseFocus(false);
        // searchField.setTextColor(16777215);
        updateCreativeSearch();
    }

    /**
     * Called when the screen is unloaded. Used to disable keyboard repeat events
     */
    @Override
    public void onGuiClosed(){
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    /**
     * Fired when a key is typed. This is the equivalent of KeyListener.keyTyped(KeyEvent e).
     */
    @Override
    protected void keyTyped(char par1, int par2){
        if(searchField.textboxKeyTyped(par1, par2)) {
            updateCreativeSearch();
        } else {
            super.keyTyped(par1, par2);
        }
    }

    private void updateCreativeSearch(){
        ContainerBlockWiki containercreative = (ContainerBlockWiki)inventorySlots;
        containercreative.itemList.clear();

        Item[] aitem = Item.itemsList;
        int i = aitem.length;
        int j;

        for(j = 0; j < i; ++j) {
            Item item = aitem[j];

            if(item != null && item.getCreativeTab() != null) {
                item.getSubItems(item.itemID, (CreativeTabs)null, containercreative.itemList);
            }
        }

        Enchantment[] aenchantment = Enchantment.enchantmentsList;
        i = aenchantment.length;

        for(j = 0; j < i; ++j) {
            Enchantment enchantment = aenchantment[j];

            if(enchantment != null && enchantment.type != null) {
                Item.enchantedBook.func_92113_a(enchantment, containercreative.itemList);
            }
        }
        updateFilteredItems(containercreative);
    }

    //split from above for custom search tabs
    private void updateFilteredItems(ContainerBlockWiki containercreative){
        Iterator iterator = containercreative.itemList.iterator();
        String s = searchField.getText().toLowerCase();

        while(iterator.hasNext()) {
            ItemStack itemstack = (ItemStack)iterator.next();
            boolean flag = false;
            Iterator iterator1 = itemstack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips).iterator();

            while(true) {
                if(iterator1.hasNext()) {
                    String s1 = (String)iterator1.next();

                    if(!s1.toLowerCase().contains(s)) {
                        continue;
                    }

                    flag = true;
                }

                if(!flag) {
                    iterator.remove();
                }

                break;
            }
        }

        currentScroll = 0.0F;
        containercreative.scrollTo(0.0F);
    }

    /**
     * returns (if you are not on the inventoryTab) and (the flag isn't set) and( you have more than 1 page of items)
     */
    private boolean needsScrollBars(){
        return ((ContainerBlockWiki)inventorySlots).hasMoreThan1PageOfItemsInList();
    }

    /**
     * Handles mouse input.
     */
    @Override
    public void handleMouseInput(){
        super.handleMouseInput();
        int i = Mouse.getEventDWheel();

        if(i != 0 && needsScrollBars()) {
            int j = ((ContainerBlockWiki)inventorySlots).itemList.size() / 8 - 9 + 1;

            if(i > 0) {
                i = 1;
            }

            if(i < 0) {
                i = -1;
            }

            currentScroll = (float)(currentScroll - (double)i / (double)j);

            if(currentScroll < 0.0F) {
                currentScroll = 0.0F;
            }

            if(currentScroll > 1.0F) {
                currentScroll = 1.0F;
            }

            ((ContainerBlockWiki)inventorySlots).scrollTo(currentScroll);
        }
    }

    /**
     * Draws the screen and all the components in it.
     */
    @Override
    public void drawScreen(int par1, int par2, float par3){
        boolean flag = Mouse.isButtonDown(0);
        int k = guiLeft;
        int l = guiTop;
        int i1 = k + SCROLL_X;
        int j1 = l + SCROLL_Y;
        int k1 = i1 + 14;
        int l1 = j1 + SCROLL_HEIGHT;

        if(!wasClicking && flag && par1 >= i1 && par2 >= j1 && par1 < k1 && par2 < l1) {
            isScrolling = needsScrollBars();
        }

        if(!flag) {
            isScrolling = false;
        }

        wasClicking = flag;

        if(isScrolling) {
            currentScroll = (par2 - j1 - 7.5F) / (l1 - j1 - 15.0F);

            if(currentScroll < 0.0F) {
                currentScroll = 0.0F;
            }

            if(currentScroll > 1.0F) {
                currentScroll = 1.0F;
            }

            ((ContainerBlockWiki)inventorySlots).scrollTo(currentScroll);
        }

        super.drawScreen(par1, par2, par3);

        if(curEntityRenderer != null) {
            GL11.glPushMatrix();
            GL11.glTranslated(50, 50, 0);
            GL11.glScaled(20, -20, -20);
            curEntityRenderer.doRender(curEntity, 0, 0, 0, 0, 0);
            GL11.glPopMatrix();
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_LIGHTING);
    }

    @Override
    protected void drawItemStackTooltip(ItemStack par1ItemStack, int par2, int par3){
        List list = par1ItemStack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);
        CreativeTabs creativetabs = par1ItemStack.getItem().getCreativeTab();

        if(creativetabs == null && par1ItemStack.itemID == Item.enchantedBook.itemID) {
            Map map = EnchantmentHelper.getEnchantments(par1ItemStack);

            if(map.size() == 1) {
                Enchantment enchantment = Enchantment.enchantmentsList[((Integer)map.keySet().iterator().next()).intValue()];
                CreativeTabs[] acreativetabs = CreativeTabs.creativeTabArray;
                int k = acreativetabs.length;

                for(int l = 0; l < k; ++l) {
                    CreativeTabs creativetabs1 = acreativetabs[l];

                    if(creativetabs1.func_111226_a(enchantment.type)) {
                        creativetabs = creativetabs1;
                        break;
                    }
                }
            }
        }

        if(creativetabs != null) {
            list.add(1, "" + EnumChatFormatting.BOLD + EnumChatFormatting.BLUE + I18n.getString(creativetabs.getTranslatedTabLabel()));
        }

        for(int i1 = 0; i1 < list.size(); ++i1) {
            if(i1 == 0) {
                list.set(i1, "\u00a7" + Integer.toHexString(par1ItemStack.getRarity().rarityColor) + (String)list.get(i1));
            } else {
                list.set(i1, EnumChatFormatting.GRAY + (String)list.get(i1));
            }
        }

        func_102021_a(list, par2, par3);
    }

    /**
     * Draw the background layer for the GuiContainer (everything behind the items)
     */
    @Override
    protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3){
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        // RenderHelper.enableGUIStandardItemLighting();

        mc.getTextureManager().bindTexture(Textures.GUI_BLOCK_WIKI);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        searchField.drawTextBox();
        int i1 = guiLeft + SCROLL_X;
        int k = guiTop + SCROLL_Y;
        int l = k + SCROLL_HEIGHT;
        mc.getTextureManager().bindTexture(field_110424_t);
        drawTexturedModalRect(i1, k + (int)((l - k - 17) * currentScroll), 232 + (needsScrollBars() ? 0 : 12), 0, 12, 15);
        drawSelectedStack();
        drawWikiPage();

    }

    private void drawWikiPage(){
        GL11.glPushMatrix();
        GL11.glTranslated(guiLeft + TEXT_START_X, TEXT_START_Y + guiTop, 0);
        GL11.glScaled(TEXT_SCALE, TEXT_SCALE, 1);
        for(LocatedString locatedString : locatedStrings) {
            fontRenderer.drawString(locatedString.string, locatedString.x, locatedString.y, locatedString.color, locatedString.shadow);
        }
        GL11.glPopMatrix();
        GL11.glPushMatrix();
        GL11.glTranslated(guiLeft, guiTop, 0);
        for(LocatedStack locatedStack : locatedStacks) {
            itemRenderer.renderItemAndEffectIntoGUI(fontRenderer, mc.getTextureManager(), locatedStack.stack, locatedStack.x, locatedStack.y);
        }
        GL11.glPopMatrix();
    }

    private void drawSelectedStack(){
        GL11.glPushMatrix();
        GL11.glScaled(2.2, 2.2, 2.2);
        customItemRenderer.renderItemAndEffectIntoGUI(fontRenderer, mc.getTextureManager(), drawingStack, guiLeft / 2 + 20, guiTop / 2 + 3);
        // itemRenderer.renderItemOverlayIntoGUI(fontRenderer, mc.getTextureManager(), drawingStack, 2, 2, s);
        GL11.glPopMatrix();
    }

    private void updateWikiPage(){
        locatedStacks.clear();
        locatedStrings.clear();
        List<String> wrappedInfo = new ArrayList<String>();
        for(String line : fileInfo) {
            wrappedInfo.addAll(Arrays.asList(WordUtils.wrap(line, WRAP_LENGTH).split(System.getProperty("line.separator"))));
        }
        int yOffset = 0;
        for(int i = 0; i < wrappedInfo.size(); i++) {
            int posY = i * fontRenderer.FONT_HEIGHT + 1;
            int firstIndex = locatedStrings.size();
            int off = updateWikiString(wrappedInfo.get(i), 0, posY + yOffset, 0xFF000000, false);
            for(int j = firstIndex; j < locatedStrings.size(); j++) {
                LocatedString locatedString = locatedStrings.get(j);
                locatedString.y += off / 2;
            }
            yOffset += off;
        }
    }

    private int updateWikiString(String line, int posX, int posY, int color, boolean shadow){
        String[] splitLine = line.split("\\[", 2);
        locatedStrings.add(new LocatedString(splitLine[0], posX, posY, color, shadow));
        if(splitLine.length > 1) {
            String[] remainder = splitLine[1].split("\\]", 2);
            int imageX = posX + fontRenderer.getStringWidth(splitLine[0]) + 2;
            Rectangle rect = decomposeCode(remainder[0], imageX, posY);
            if(remainder.length > 1) {
                return Math.max(rect.height - fontRenderer.FONT_HEIGHT, updateWikiString(remainder[1], imageX + rect.width + 2, posY, color, shadow));
            } else {
                updateWikiString("ERROR: No closing ']' found!", imageX + rect.width + 2, posY, 0xFF0000FF, false);
            }
        }
        return 0;
    }

    private Rectangle decomposeCode(String code, int x, int y){
        ItemStack stack = WikiUtils.getStackFromName(code);
        if(stack != null) {
            locatedStacks.add(new LocatedStack(stack, TEXT_START_X + x / 2, TEXT_START_Y + y / 2));
        }
        return new Rectangle(36, 36);
    }

    /**
     * Renders the creative inventory hovering text if mouse is over it. Returns true if did render or false otherwise.
     * Params: current creative tab to be checked, current mouse x position, current mouse y position.
     */
    protected boolean renderCreativeInventoryHoveringText(CreativeTabs par1CreativeTabs, int par2, int par3){
        int k = par1CreativeTabs.getTabColumn();
        int l = 28 * k;
        byte b0 = 0;

        if(k == 5) {
            l = xSize - 28 + 2;
        } else if(k > 0) {
            l += k;
        }

        int i1;

        if(par1CreativeTabs.isTabInFirstRow()) {
            i1 = b0 - 32;
        } else {
            i1 = b0 + ySize;
        }

        if(isPointInRegion(l + 3, i1 + 3, 23, 27, par2, par3)) {
            drawCreativeTabHoveringText(I18n.getString(par1CreativeTabs.getTranslatedTabLabel()), par2, par3);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Renders passed creative inventory tab into the screen.
     */
    /*
    protected void renderCreativeTab(CreativeTabs par1CreativeTabs){
        boolean flag = par1CreativeTabs.getTabIndex() == selectedTabIndex;
        boolean flag1 = par1CreativeTabs.isTabInFirstRow();
        int i = par1CreativeTabs.getTabColumn();
        int j = i * 28;
        int k = 0;
        int l = guiLeft + 28 * i;
        int i1 = guiTop;
        byte b0 = 32;

        if(flag) {
            k += 32;
        }

        if(i == 5) {
            l = guiLeft + xSize - 28;
        } else if(i > 0) {
            l += i;
        }

        if(flag1) {
            i1 -= 28;
        } else {
            k += 64;
            i1 += ySize - 4;
        }

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor3f(1F, 1F, 1F); //Forge: Reset color in case Items change it.
        drawTexturedModalRect(l, i1, j, k, 28, b0);
        zLevel = 100.0F;
        itemRenderer.zLevel = 100.0F;
        l += 6;
        i1 += 8 + (flag1 ? 1 : -1);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        ItemStack itemstack = par1CreativeTabs.getIconItemStack();
        itemRenderer.renderItemAndEffectIntoGUI(fontRenderer, mc.getTextureManager(), itemstack, l, i1);
        itemRenderer.renderItemOverlayIntoGUI(fontRenderer, mc.getTextureManager(), itemstack, l, i1);
        GL11.glDisable(GL11.GL_LIGHTING);
        itemRenderer.zLevel = 0.0F;
        zLevel = 0.0F;
    }*/

    /**
     * Returns the creative inventory
     */
    static InventoryBasic getInventory(){
        return inventory;
    }
}
